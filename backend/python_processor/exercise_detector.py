import cv2
import mediapipe as mp
import base64
import numpy as np
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import time
import os

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

class HandDetector:
    def __init__(self):
        print("HandDetector инициализирован")
        # Создаем папку для отладки
        if not os.path.exists('debug_frames'):
            os.makedirs('debug_frames')

    def fix_base64_padding(self, data):
        """Исправляет padding в base64 строке"""
        missing_padding = len(data) % 4
        if missing_padding:
            data += '=' * (4 - missing_padding)
        return data

    def process_frame(self, frame_data):
        """Обработка кадра"""
        try:
            print("\n=== НОВЫЙ КАДР ===")

            # Декодируем base64
            if isinstance(frame_data, str):
                try:
                    # Исправляем padding
                    frame_data = self.fix_base64_padding(frame_data)
                    frame_bytes = base64.b64decode(frame_data)
                    print(f"📦 Декодировано {len(frame_bytes)} байт")
                except Exception as e:
                    print(f"❌ Ошибка декодирования base64: {e}")
                    return {
                        "fist_detected": False,
                        "hand_detected": False,
                        "raised_fingers": 0,
                        "finger_states": [False, False, False, False, False],
                        "message": f"Ошибка декодирования: {str(e)}",
                        "processed_frame": "",
                        "status": "error"
                    }
            else:
                print(f"❌ Неожиданный тип данных: {type(frame_data)}")
                return {
                    "fist_detected": False,
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "finger_states": [False, False, False, False, False],
                    "message": "Invalid frame data type",
                    "processed_frame": "",
                    "status": "error"
                }

            # Конвертируем байты в изображение
            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if frame is None:
                print("❌ Не удалось декодировать изображение")
                return {
                    "fist_detected": False,
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "finger_states": [False, False, False, False, False],
                    "message": "Cannot decode image",
                    "processed_frame": "",
                    "status": "error"
                }

            print(f"📷 Изображение: {frame.shape}")

            # Конвертируем в RGB для MediaPipe
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(frame_rgb)

            # Создаем копию для визуализации
            display_frame = frame.copy()
            h, w, _ = frame.shape

            hand_detected = False
            raised_fingers = 0
            finger_states = [False, False, False, False, False]

            if results.multi_hand_landmarks:
                hand_detected = True
                print("✅ Рука обнаружена!")

                for hand_landmarks in results.multi_hand_landmarks:
                    # Рисуем скелет
                    mp_drawing.draw_landmarks(
                        display_frame,
                        hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_drawing_styles.get_default_hand_landmarks_style(),
                        mp_drawing_styles.get_default_hand_connections_style()
                    )

                    # Определяем поднятые пальцы
                    finger_tips = [4, 8, 12, 16, 20]
                    finger_pips = [3, 6, 10, 14, 18]

                    # Получаем координаты для отрисовки
                    tip_positions = []
                    for i in range(5):
                        tip = hand_landmarks.landmark[finger_tips[i]]
                        pip = hand_landmarks.landmark[finger_pips[i]]
                        x, y = int(tip.x * w), int(tip.y * h)
                        tip_positions.append((x, y))

                        if i == 0:  # Большой палец
                            index_mcp = hand_landmarks.landmark[5]
                            dist = abs(tip.x - index_mcp.x) + abs(tip.y - index_mcp.y)
                            finger_states[i] = dist > 0.15
                        else:
                            finger_states[i] = tip.y < pip.y - 0.02

                        if finger_states[i]:
                            raised_fingers += 1

                    # Рисуем точки на кончиках пальцев
                    colors = [(255, 0, 255), (255, 0, 0), (0, 255, 0), (0, 255, 255), (0, 0, 255)]

                    for i, (x, y) in enumerate(tip_positions):
                        # Цвет: зеленый если палец поднят, красный если сжат
                        color = (0, 255, 0) if finger_states[i] else (0, 0, 255)

                        # Рисуем круг
                        cv2.circle(display_frame, (x, y), 20, color, -1)
                        cv2.circle(display_frame, (x, y), 20, (255, 255, 255), 2)

                        # Номер пальца и статус
                        status = "⬆️" if finger_states[i] else "⬇️"
                        cv2.putText(display_frame, f"{i}{status}", (x-20, y-25),
                                   cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

                    # Информация на кадре
                    cv2.rectangle(display_frame, (5, 5), (400, 100), (0, 0, 0), -1)
                    cv2.rectangle(display_frame, (5, 5), (400, 100), (255, 255, 255), 2)

                    info_y = 30
                    cv2.putText(display_frame, f"Пальцев поднято: {raised_fingers}/5", (15, info_y),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

                    info_y += 25
                    status_text = " ".join([f"{i}:{'⬆️' if s else '⬇️'}" for i, s in enumerate(finger_states)])
                    cv2.putText(display_frame, status_text, (15, info_y),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

                    info_y += 25
                    if raised_fingers <= 1:
                        message = "✅ КУЛАК СЖАТ"
                        color = (0, 255, 0)
                    else:
                        message = f"❌ Сожмите пальцы ({raised_fingers}/5)"
                        color = (0, 0, 255)

                    cv2.putText(display_frame, message, (15, info_y),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

                cv2.putText(display_frame, "✅ РУКА", (10, h-20),
                           cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            else:
                print("❌ Рука не обнаружена")
                cv2.rectangle(display_frame, (5, 5), (200, 50), (0, 0, 0), -1)
                cv2.putText(display_frame, "❌ НЕТ РУКИ", (15, 35),
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

            # Конвертируем обратно в base64
            _, buffer = cv2.imencode('.jpg', display_frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
            frame_bytes_out = base64.b64encode(buffer).decode('utf-8')

            # Определяем сообщение
            if hand_detected:
                if raised_fingers <= 1:
                    message = "Кулак сжат правильно!"
                else:
                    message = f"Сожмите пальцы (поднято {raised_fingers})"
            else:
                message = "Рука не обнаружена"

            result = {
                "fist_detected": hand_detected and raised_fingers <= 1,
                "hand_detected": hand_detected,
                "raised_fingers": raised_fingers,
                "finger_states": finger_states,
                "message": message,
                "processed_frame": frame_bytes_out,
                "status": "success"
            }

            print(f"📤 Отправка: hand_detected={hand_detected}, пальцев={raised_fingers}, сообщение='{message}'")
            return result

        except Exception as e:
            print(f"❌ Критическая ошибка: {e}")
            import traceback
            traceback.print_exc()

            return {
                "fist_detected": False,
                "hand_detected": False,
                "raised_fingers": 0,
                "finger_states": [False, False, False, False, False],
                "message": f"Ошибка: {str(e)}",
                "processed_frame": "",
                "status": "error"
            }

detector = HandDetector()

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "time": time.time()})

@app.route('/process', methods=['POST'])
def process_frame():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), 400

        frame = data.get('frame')
        if not frame:
            return jsonify({"error": "No frame provided"}), 400

        result = detector.process_frame(frame)
        return jsonify(result)
    except Exception as e:
        print(f"❌ Ошибка в /process: {e}")
        return jsonify({
            "fist_detected": False,
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"Ошибка сервера: {str(e)}",
            "processed_frame": "",
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
            frame = data.get('frame')
            if frame:
                result = detector.process_frame(frame)
                emit('feedback', result)
            else:
                emit('feedback', {
                    "fist_detected": False,
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "message": "No frame data",
                    "processed_frame": "",
                    "status": "error"
                })
        else:
            emit('feedback', {
                "fist_detected": False,
                "hand_detected": False,
                "raised_fingers": 0,
                "message": "Invalid data format",
                "processed_frame": "",
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
            "status": "error"
        })

if __name__ == '__main__':
    print("=" * 60)
    print("🤚 Python Processor запущен")
    print("=" * 60)
    print("📡 HTTP:  http://localhost:5001")
    print("📡 WS:    ws://localhost:5001")
    print("📁 debug: debug_frames/")
    print("=" * 60)
    socketio.run(app, host='0.0.0.0', port=5001, debug=True, allow_unsafe_werkzeug=True)