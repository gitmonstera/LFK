import cv2
import mediapipe as mp
import base64
import numpy as np
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import time
import os
from enum import Enum

# Настраиваем логирование
logging.basicConfig(level=logging.INFO)
log = logging.getLogger('werkzeug')
log.setLevel(logging.ERROR)

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*", ping_timeout=60, ping_interval=25)

# Инициализация MediaPipe
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles

hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

class ExerciseType(Enum):
    FIST = "fist"           # Кулак
    FIST_INDEX = "fist-index"  # Кулак с указательным

class HandDetector:
    def __init__(self):
        self.current_exercise = ExerciseType.FIST
        print(f"HandDetector инициализирован")
        # Создаем папку для отладки
        if not os.path.exists('debug_frames'):
            os.makedirs('debug_frames')

    def set_exercise_from_url(self, exercise_type):
        """Устанавливает упражнение на основе URL"""
        try:
            self.current_exercise = ExerciseType(exercise_type)
            print(f"🔄 Установлено упражнение: {self.current_exercise.value}")
            return True
        except:
            print(f"❌ Неизвестный тип: {exercise_type}")
            return False

    def check_exercise(self, finger_states):
        """Проверяет правильность выполнения упражнения"""
        raised = sum(finger_states)

        if self.current_exercise == ExerciseType.FIST:
            # EX1: Кулак - все пальцы сжаты (поднято 0-1 палец)
            is_correct = raised <= 1
            message = "✅ Кулак сжат!" if is_correct else f"❌ Сожмите пальцы ({raised} поднято)"

        elif self.current_exercise == ExerciseType.FIST_INDEX:
            # EX2: Кулак с указательным
            index_raised = finger_states[1]
            other_raised = any([finger_states[2], finger_states[3], finger_states[4]])

            is_correct = index_raised and not other_raised

            if is_correct:
                message = "✅ Указательный поднят!"
            elif not index_raised:
                message = "❌ Поднимите указательный"
            else:
                message = "❌ Сожмите остальные пальцы"
        else:
            is_correct = False
            message = "Неизвестное упражнение"

        return is_correct, message

    def fix_base64_padding(self, data):
        """Исправляет padding в base64 строке"""
        # Убираем возможные кавычки
        data = data.strip('"')
        # Добавляем padding если нужно
        missing_padding = len(data) % 4
        if missing_padding:
            data += '=' * (4 - missing_padding)
        return data

    def process_frame(self, frame_data):
        """Обработка кадра"""
        try:
            print(f"\n=== НОВЫЙ КАДР ({self.current_exercise.value}) ===")

            # Если пришел словарь, извлекаем frame
            if isinstance(frame_data, dict):
                if 'frame' in frame_data:
                    frame_data = frame_data['frame']
                else:
                    return self.error_response("No frame in data")

            # Декодируем base64
            if isinstance(frame_data, str):
                try:
                    # Исправляем padding
                    frame_data = self.fix_base64_padding(frame_data)
                    frame_bytes = base64.b64decode(frame_data)
                    print(f"📦 Декодировано {len(frame_bytes)} байт")
                except Exception as e:
                    print(f"❌ Ошибка декодирования base64: {e}")
                    print(f"Первые 50 символов: {frame_data[:50]}")
                    return self.error_response(f"Ошибка декодирования")
            else:
                return self.error_response("Invalid frame data type")

            # Конвертируем байты в изображение
            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if frame is None:
                print("❌ Не удалось декодировать изображение")
                return self.error_response("Cannot decode image")

            print(f"📷 Изображение: {frame.shape}")

            # Конвертируем в RGB для MediaPipe
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(frame_rgb)

            # Создаем копию для визуализации
            display_frame = frame.copy()
            h, w, _ = frame.shape

            if results.multi_hand_landmarks:
                return self.process_hand(results, display_frame, h, w)
            else:
                return self.no_hand_response(display_frame)

        except Exception as e:
            print(f"❌ Критическая ошибка: {e}")
            import traceback
            traceback.print_exc()
            return self.error_response(str(e))

    def get_finger_states(self, hand_landmarks, w, h):
        """Получает состояние пальцев"""
        finger_tips = [4, 8, 12, 16, 20]
        finger_pips = [3, 6, 10, 14, 18]

        finger_states = []
        tip_positions = []

        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            pip = hand_landmarks.landmark[finger_pips[i]]
            x, y = int(tip.x * w), int(tip.y * h)
            tip_positions.append((x, y))

            if i == 0:  # Большой палец
                index_mcp = hand_landmarks.landmark[5]
                dist = abs(tip.x - index_mcp.x) + abs(tip.y - index_mcp.y)
                finger_states.append(dist > 0.15)
            else:
                finger_states.append(tip.y < pip.y - 0.02)

        return finger_states, tip_positions

    def process_hand(self, results, display_frame, h, w):
        """Обрабатывает кадр с рукой"""
        for hand_landmarks in results.multi_hand_landmarks:
            # Рисуем скелет
            mp_drawing.draw_landmarks(
                display_frame,
                hand_landmarks,
                mp_hands.HAND_CONNECTIONS,
                mp_drawing_styles.get_default_hand_landmarks_style(),
                mp_drawing_styles.get_default_hand_connections_style()
            )

            finger_states, tip_positions = self.get_finger_states(hand_landmarks, w, h)
            raised = sum(finger_states)
            is_correct, message = self.check_exercise(finger_states)

            print(f"   Пальцы: {['⬆️' if s else '⬇️' for s in finger_states]}")
            print(f"   Результат: {message}")

            # Рисуем точки на кончиках пальцев
            colors = [(255, 0, 255), (255, 0, 0), (0, 255, 0), (0, 255, 255), (0, 0, 255)]

            for i, (x, y) in enumerate(tip_positions):
                # Определяем цвет в зависимости от упражнения
                if self.current_exercise == ExerciseType.FIST:
                    # Для кулака: зеленый если палец сжат, красный если поднят
                    color = (0, 255, 0) if not finger_states[i] else (0, 0, 255)
                else:  # FIST_INDEX
                    # Для указательного пальца: зеленый если поднят, красный если сжат
                    if i == 1:  # Указательный
                        color = (0, 255, 0) if finger_states[i] else (0, 0, 255)
                    else:  # Остальные пальцы
                        color = (0, 255, 0) if not finger_states[i] else (0, 0, 255)

                # Рисуем круг
                cv2.circle(display_frame, (x, y), 20, color, -1)
                cv2.circle(display_frame, (x, y), 20, (255, 255, 255), 2)

                # Номер пальца и статус
                status = "⬆️" if finger_states[i] else "⬇️"
                cv2.putText(display_frame, f"{i}{status}", (x-20, y-25),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

            # Информация на кадре
            cv2.rectangle(display_frame, (5, 5), (450, 130), (0, 0, 0), -1)
            cv2.rectangle(display_frame, (5, 5), (450, 130), (255, 255, 255), 2)

            ex_name = "Кулак" if self.current_exercise == ExerciseType.FIST else "Кулак + указательный"
            cv2.putText(display_frame, f"Упражнение: {ex_name}", (15, 30),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
            cv2.putText(display_frame, f"Пальцев: {raised}/5", (15, 55),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

            color = (0, 255, 0) if is_correct else (0, 0, 255)
            cv2.putText(display_frame, message, (15, 85),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

        return self.success_response(display_frame, True, raised, finger_states, message)

    def no_hand_response(self, display_frame):
        """Ответ когда нет руки"""
        cv2.rectangle(display_frame, (5, 5), (200, 50), (0, 0, 0), -1)
        cv2.putText(display_frame, "❌ НЕТ РУКИ", (15, 35),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        return self.success_response(display_frame, False, 0, [False]*5, "Рука не обнаружена")

    def success_response(self, frame, hand_detected, raised, states, message):
        """Формирует успешный ответ"""
        try:
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
            frame_out = base64.b64encode(buffer).decode('utf-8')

            return {
                "fist_detected": hand_detected and (raised <= 1 if self.current_exercise == ExerciseType.FIST else (states[1] and not any(states[2:]))),
                "hand_detected": hand_detected,
                "raised_fingers": raised,
                "finger_states": states,
                "message": message,
                "processed_frame": frame_out,
                "current_exercise": self.current_exercise.value,
                "status": "success"
            }
        except Exception as e:
            print(f"❌ Ошибка при формировании ответа: {e}")
            return self.error_response("Error creating response")

    def error_response(self, message):
        return {
            "fist_detected": False,
            "hand_detected": False,
            "raised_fingers": 0,
            "finger_states": [False]*5,
            "message": message,
            "processed_frame": "",
            "current_exercise": self.current_exercise.value,
            "status": "error"
        }

# Создаем детектор
detector = HandDetector()

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "current_exercise": detector.current_exercise.value
    })

@app.route('/process', methods=['POST'])
def process_frame():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), 400

        result = detector.process_frame(data)
        return jsonify(result)
    except Exception as e:
        print(f"❌ Ошибка в /process: {e}")
        return jsonify({
            "fist_detected": False,
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"Server error: {str(e)}",
            "processed_frame": "",
            "current_exercise": detector.current_exercise.value,
            "status": "error"
        }), 500

@socketio.on('connect')
def handle_connect():
    print('🔌 Клиент подключен')

@socketio.on('disconnect')
def handle_disconnect():
    print('🔌 Клиент отключен')

@socketio.on('frame')
def handle_frame(data):
    try:
        if isinstance(data, dict):
            result = detector.process_frame(data)
            emit('feedback', result)
        else:
            emit('feedback', {
                "fist_detected": False,
                "hand_detected": False,
                "raised_fingers": 0,
                "message": "Invalid data format",
                "processed_frame": "",
                "current_exercise": detector.current_exercise.value,
                "status": "error"
            })
    except Exception as e:
        print(f"❌ WebSocket error: {e}")

if __name__ == '__main__':
    print("=" * 60)
    print("🤚 Python Processor")
    print("=" * 60)
    print("📡 Сервер: http://localhost:5001")
    print("\n📋 Поддерживаемые упражнения:")
    print("   fist - Кулак")
    print("   fist-index - Кулак с указательным")
    print("=" * 60)
    socketio.run(app, host='0.0.0.0', port=5001, debug=True, allow_unsafe_werkzeug=True)