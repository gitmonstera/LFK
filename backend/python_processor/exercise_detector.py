import cv2
import mediapipe as mp
import base64
import numpy as np
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import time
import os

# Импортируем упражнения
from exercises import EXERCISE_CLASSES

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

class ExerciseManager:
    """Менеджер упражнений"""

    def __init__(self):
        self.exercises = {}
        self.current_exercise = None
        self.current_exercise_id = "fist"

        # Загружаем все доступные упражнения
        self.load_exercises()

        # Устанавливаем упражнение по умолчанию
        self.set_exercise("fist")

        # Создаем папку для отладки
        if not os.path.exists('debug_frames'):
            os.makedirs('debug_frames')

    def load_exercises(self):
        """Загружает все упражнения из папки exercises"""
        for ex_id, ex_class in EXERCISE_CLASSES.items():
            self.exercises[ex_id] = ex_class()
            print(f"📚 Загружено упражнение: {ex_id} - {self.exercises[ex_id].name}")

    def set_exercise(self, exercise_id):
        """Устанавливает текущее упражнение"""
        if exercise_id in self.exercises:
            self.current_exercise = self.exercises[exercise_id]
            self.current_exercise_id = exercise_id
            # Сбрасываем упражнение при смене
            if hasattr(self.current_exercise, 'reset'):
                self.current_exercise.reset()
            print(f"🔄 Текущее упражнение: {self.current_exercise.name}")
            return True
        else:
            print(f"❌ Упражнение {exercise_id} не найдено")
            return False

    def reset_current_exercise(self):
        """Сбрасывает текущее упражнение в начальное состояние"""
        if self.current_exercise and hasattr(self.current_exercise, 'reset'):
            self.current_exercise.reset()
            print(f"🔄 Текущее упражнение сброшено")

    def get_exercise_list(self):
        """Возвращает список доступных упражнений"""
        return [{"id": ex_id, "name": ex.name} for ex_id, ex in self.exercises.items()]

    def process_frame(self, frame_data):
        """Обработка кадра"""
        try:
            print(f"\n=== НОВЫЙ КАДР ({self.current_exercise.name}) ===")

            # Декодируем base64
            if isinstance(frame_data, str):
                try:
                    # Исправляем padding
                    missing_padding = len(frame_data) % 4
                    if missing_padding:
                        frame_data += '=' * (4 - missing_padding)

                    frame_bytes = base64.b64decode(frame_data)
                    print(f"📦 Декодировано {len(frame_bytes)} байт")
                except Exception as e:
                    print(f"❌ Ошибка декодирования base64: {e}")
                    return self.error_response(f"Ошибка декодирования")
            else:
                return self.error_response("Invalid frame data type")

            # Конвертируем байты в изображение
            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if frame is None:
                return self.error_response("Cannot decode image")

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

    def process_hand(self, results, display_frame, h, w):
        """Обрабатывает кадр с рукой"""
        raised_fingers = 0
        finger_states = []

        for hand_landmarks in results.multi_hand_landmarks:
            # Рисуем скелет
            mp_drawing.draw_landmarks(
                display_frame,
                hand_landmarks,
                mp_hands.HAND_CONNECTIONS,
                mp_drawing_styles.get_default_hand_landmarks_style(),
                mp_drawing_styles.get_default_hand_connections_style()
            )

            # Получаем состояние пальцев
            finger_states, tip_positions = self.current_exercise.get_finger_states(
                hand_landmarks, (h, w, 3)
            )

            # Проверяем упражнение
            is_correct, message = self.current_exercise.check_fingers(
                finger_states, hand_landmarks, (h, w, 3)
            )

            # Рисуем обратную связь
            display_frame = self.current_exercise.draw_feedback(
                display_frame, finger_states, tip_positions, is_correct, message
            )

            raised_fingers = sum(finger_states)
            print(f"   Пальцы: {['⬆️' if s else '⬇️' for s in finger_states]}")
            print(f"   Результат: {message}")

        return self.success_response(display_frame, True, raised_fingers, finger_states, message)

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

            # Базовый ответ
            response = {
                "fist_detected": hand_detected,
                "hand_detected": hand_detected,
                "raised_fingers": raised,
                "finger_states": states,
                "message": message,
                "processed_frame": frame_out,
                "current_exercise": self.current_exercise_id,
                "exercise_name": self.current_exercise.name,
                "status": "success"
            }

            # Добавляем структурированные данные для специальных упражнений
            if hasattr(self.current_exercise, 'get_structured_data'):
                structured = self.current_exercise.get_structured_data()
                if structured:
                    response["structured"] = structured
                    print(f"📊 Добавлены структурированные данные: {structured}")

            return response
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
            "current_exercise": self.current_exercise_id,
            "exercise_name": self.current_exercise.name if self.current_exercise else "unknown",
            "status": "error"
        }

# Создаем менеджер упражнений
exercise_manager = ExerciseManager()

@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "current_exercise": exercise_manager.current_exercise_id,
        "available_exercises": exercise_manager.get_exercise_list()
    })

@app.route('/exercises', methods=['GET'])
def list_exercises():
    """Возвращает список доступных упражнений"""
    return jsonify({
        "exercises": exercise_manager.get_exercise_list()
    })

@app.route('/reset_exercise', methods=['POST'])
def reset_exercise():
    """Сбрасывает текущее упражнение"""
    try:
        exercise_manager.reset_current_exercise()
        return jsonify({
            "status": "success",
            "message": "Exercise reset successfully"
        })
    except Exception as e:
        return jsonify({"error": str(e), "status": "error"}), 400

@app.route('/set_exercise', methods=['POST'])
def set_exercise():
    """Смена упражнения"""
    try:
        data = request.get_json()
        exercise_id = data.get('exercise_id')

        if exercise_manager.set_exercise(exercise_id):
            return jsonify({
                "status": "success",
                "current_exercise": exercise_manager.current_exercise_id,
                "exercise_name": exercise_manager.current_exercise.name
            })
        else:
            return jsonify({
                "status": "error",
                "message": f"Exercise {exercise_id} not found"
            }), 400
    except Exception as e:
        return jsonify({"error": str(e), "status": "error"}), 400

@app.route('/process', methods=['POST'])
def process_frame():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), 400

        # Проверяем, есть ли смена упражнения
        if 'exercise_type' in data:
            exercise_manager.set_exercise(data['exercise_type'])

        frame = data.get('frame')
        if not frame:
            return jsonify({"error": "No frame provided"}), 400

        result = exercise_manager.process_frame(frame)
        return jsonify(result)
    except Exception as e:
        print(f"❌ Ошибка в /process: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({
            "fist_detected": False,
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"Server error: {str(e)}",
            "processed_frame": "",
            "current_exercise": exercise_manager.current_exercise_id,
            "status": "error"
        }), 500

@socketio.on('connect')
def handle_connect():
    print('🔌 Клиент подключен')
    # Сбрасываем упражнение при новом подключении
    exercise_manager.reset_current_exercise()

@socketio.on('disconnect')
def handle_disconnect():
    print('🔌 Клиент отключен')

@socketio.on('frame')
def handle_frame(data):
    try:
        if isinstance(data, dict):
            # Проверяем смену упражнения
            if 'exercise_type' in data:
                exercise_manager.set_exercise(data['exercise_type'])

            # Проверяем сброс упражнения
            if 'reset' in data and data['reset']:
                exercise_manager.reset_current_exercise()

            frame = data.get('frame')
            if frame:
                result = exercise_manager.process_frame(frame)
                emit('feedback', result)
            else:
                emit('feedback', {
                    "fist_detected": False,
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "message": "No frame data",
                    "processed_frame": "",
                    "current_exercise": exercise_manager.current_exercise_id,
                    "status": "error"
                })
        else:
            emit('feedback', {
                "fist_detected": False,
                "hand_detected": False,
                "raised_fingers": 0,
                "message": "Invalid data format",
                "processed_frame": "",
                "current_exercise": exercise_manager.current_exercise_id,
                "status": "error"
            })
    except Exception as e:
        print(f"❌ WebSocket error: {e}")
        emit('feedback', {
            "fist_detected": False,
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"WebSocket error: {str(e)}",
            "processed_frame": "",
            "current_exercise": exercise_manager.current_exercise_id,
            "status": "error"
        })

if __name__ == '__main__':
    print("=" * 60)
    print("🤚 Python Processor с модульными упражнениями")
    print("=" * 60)
    print("📡 Сервер: http://localhost:5001")
    print("\n📋 Доступные упражнения:")
    for ex in exercise_manager.get_exercise_list():
        print(f"   - {ex['id']}: {ex['name']}")
    print("=" * 60)
    socketio.run(app, host='0.0.0.0', port=5001, debug=True, allow_unsafe_werkzeug=True)