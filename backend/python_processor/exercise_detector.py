import cv2
import mediapipe as mp
import base64
import numpy as np
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import time
import random
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

class FistDetector:
    def __init__(self):
        print("FistDetector с MediaPipe инициализирован")
        # Создаем папку для отладки если её нет
        if not os.path.exists('debug_frames'):
            os.makedirs('debug_frames')

    def is_fist(self, hand_landmarks, frame_shape):
        """Упрощенное определение кулака"""
        h, w, _ = frame_shape

        # Индексы кончиков пальцев
        finger_tips = [4, 8, 12, 16, 20]
        finger_pips = [3, 6, 10, 14, 18]  # Средние суставы

        # Получаем координаты
        tips = []
        pips = []

        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            pip = hand_landmarks.landmark[finger_pips[i]]

            tips.append((tip.x, tip.y))
            pips.append((pip.x, pip.y))

        # Определяем состояние пальцев
        finger_states = []  # False = сжат, True = поднят
        raised_count = 0

        for i in range(5):
            if i == 0:  # Большой палец
                # Большой палец считаем поднятым если он далеко от указательного
                index_mcp = hand_landmarks.landmark[5]  # Основание указательного
                dist_to_index = abs(tips[0][0] - index_mcp.x) + abs(tips[0][1] - index_mcp.y)
                is_raised = dist_to_index > 0.15
            else:
                # Для остальных пальцев: поднят если кончик выше среднего сустава
                # В координатах MediaPipe Y увеличивается вниз
                is_raised = tips[i][1] < pips[i][1] - 0.02

            finger_states.append(is_raised)
            if is_raised:
                raised_count += 1

        # Кулак = поднято не более 1 пальца
        is_fist = raised_count <= 1

        # Координаты для отрисовки в пикселях
        tip_positions = []
        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            x = int(tip.x * w)
            y = int(tip.y * h)
            tip_positions.append((x, y))

        return is_fist, raised_count, finger_states, tip_positions

    def process_frame(self, frame_data):
        """Обработка кадра из base64 строки"""
        try:
            # Логируем полученные данные
            print(f"\n--- Обработка кадра ---")

            # Если пришла строка, это может быть JSON или прямая base64
            if isinstance(frame_data, str):
                # Проверяем, не является ли это JSON строкой
                try:
                    # Пробуем распарсить как JSON
                    data = json.loads(frame_data)
                    if 'frame' in data:
                        frame_data = data['frame']
                        print("📦 Извлекли frame из JSON")
                except:
                    # Если не JSON, значит это прямая base64 строка
                    pass

                # Декодируем base64
                try:
                    # Добавляем паддинг если нужно
                    missing_padding = len(frame_data) % 4
                    if missing_padding:
                        frame_data += '=' * (4 - missing_padding)

                    frame_bytes = base64.b64decode(frame_data)
                    print(f"📦 Декодировали base64, размер: {len(frame_bytes)} байт")
                except Exception as e:
                    print(f"❌ Ошибка декодирования base64: {e}")
                    return {"error": f"Base64 decode error: {e}", "status": "error"}
            else:
                print(f"❌ Неожиданный тип данных: {type(frame_data)}")
                return {"error": "Invalid frame data type", "status": "error"}

            # Конвертируем байты в numpy array
            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if frame is None:
                return {"error": "Не удалось декодировать изображение", "status": "error"}

            print(f"📷 Размер кадра: {frame.shape}")

            # Конвертируем в RGB для MediaPipe
            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = hands.process(frame_rgb)

            # Создаем копию для визуализации
            display_frame = frame.copy()
            h, w, _ = frame.shape

            hand_detected = False
            is_fist_result = False
            raised_fingers = 0
            finger_states = []
            tip_positions = []
            message = "Рука не обнаружена"

            if results.multi_hand_landmarks:
                hand_detected = True
                print("🖐️ Рука обнаружена!")

                for hand_landmarks in results.multi_hand_landmarks:
                    # Рисуем скелет руки
                    mp_drawing.draw_landmarks(
                        display_frame,
                        hand_landmarks,
                        mp_hands.HAND_CONNECTIONS,
                        mp_drawing_styles.get_default_hand_landmarks_style(),
                        mp_drawing_styles.get_default_hand_connections_style()
                    )

                    # Определяем кулак
                    is_fist_result, raised_fingers, finger_states, tip_positions = self.is_fist(hand_landmarks, frame.shape)

                    print(f"   Поднято пальцев: {raised_fingers}")
                    print(f"   Состояние: {['⬆️' if s else '⬇️' for s in finger_states]}")

                    # Рисуем кончики пальцев
                    colors = [(255, 0, 255), (255, 0, 0), (0, 255, 0), (0, 255, 255), (0, 0, 255)]
                    for i, (x, y) in enumerate(tip_positions):
                        # Красный если палец поднят, зеленый если сжат
                        color = (0, 0, 255) if finger_states[i] else (0, 255, 0)

                        # Рисуем большой круг
                        cv2.circle(display_frame, (x, y), 20, color, -1)
                        cv2.circle(display_frame, (x, y), 20, (255, 255, 255), 3)

                        # Номер пальца
                        cv2.putText(display_frame, str(i), (x-10, y-25),
                                   cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

                        # Статус пальца
                        status = "⬆️" if finger_states[i] else "⬇️"
                        cv2.putText(display_frame, status, (x-15, y+30),
                                   cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

                    # Отображаем информацию на кадре
                    # Фон для текста
                    cv2.rectangle(display_frame, (5, 5), (400, 150), (0, 0, 0), -1)
                    cv2.rectangle(display_frame, (5, 5), (400, 150), (255, 255, 255), 2)

                    # Текст
                    y_offset = 30
                    cv2.putText(display_frame, f"Поднято пальцев: {raised_fingers}/5", (15, y_offset),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

                    y_offset += 25
                    status_text = " ".join([f"{i}:{'⬆️' if s else '⬇️'}" for i, s in enumerate(finger_states)])
                    cv2.putText(display_frame, status_text, (15, y_offset),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

                    y_offset += 25
                    if is_fist_result:
                        message = "✅ КУЛАК СЖАТ ПРАВИЛЬНО!"
                        color = (0, 255, 0)
                    else:
                        if raised_fingers > 1:
                            message = f"❌ Сожмите пальцы (поднято {raised_fingers})"
                        else:
                            message = "❌ Сожмите руку в кулак"
                        color = (0, 0, 255)

                    cv2.putText(display_frame, message, (15, y_offset),
                               cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

            else:
                print("❌ Рука не обнаружена")
                cv2.rectangle(display_frame, (5, 5), (300, 50), (0, 0, 0), -1)
                cv2.putText(display_frame, "❌ НЕТ РУКИ", (15, 35),
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

            # Сохраняем каждый 30-й кадр для отладки
            if random.randint(1, 30) == 1:
                timestamp = int(time.time())
                debug_filename = f"debug_frames/frame_{timestamp}.jpg"
                cv2.imwrite(debug_filename, display_frame)
                print(f"💾 Сохранен отладочный кадр: {debug_filename}")

            # Конвертируем обратно в байты
            _, buffer = cv2.imencode('.jpg', display_frame, [cv2.IMWRITE_JPEG_QUALITY, 85])
            frame_bytes_out = base64.b64encode(buffer).decode('utf-8')

            print(f"📤 Размер выходного кадра: {len(frame_bytes_out)} байт")

            result = {
                "fist_detected": bool(is_fist_result) if hand_detected else False,
                "hand_detected": bool(hand_detected),
                "raised_fingers": int(raised_fingers),
                "finger_states": finger_states,
                "message": message,
                "processed_frame": frame_bytes_out,
                "status": "success"
            }

            print(f"✅ Результат: fist={result['fist_detected']}, message='{message}'")
            return result

        except Exception as e:
            print(f"❌ Ошибка в process_frame: {e}")
            import traceback
            traceback.print_exc()
            return {"error": str(e), "status": "error"}


detector = FistDetector()

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "time": time.time()})

@app.route('/process', methods=['POST'])
def process_frame():
    try:
        data = request.get_json()
        print(f"\n📨 Получен POST запрос")

        if not data or 'frame' not in data:
            return jsonify({"error": "No frame provided"}), 400

        frame = data['frame']
        result = detector.process_frame(frame)
        return jsonify(result)
    except Exception as e:
        print(f"❌ Ошибка в /process: {e}")
        return jsonify({"error": str(e), "status": "error"}), 500

@socketio.on('connect')
def handle_connect():
    print('🔌 Клиент подключен по WebSocket')

@socketio.on('disconnect')
def handle_disconnect():
    print('🔌 Клиент отключен')

@socketio.on('frame')
def handle_frame(data):
    """Обработка кадра через WebSocket"""
    print("\n📨 Получен frame через WebSocket")
    try:
        if isinstance(data, dict) and 'frame' in data:
            result = detector.process_frame(data['frame'])
            emit('feedback', result)
            print("📤 Отправлен feedback")
        else:
            print("❌ Неверный формат данных")
            emit('feedback', {"error": "Invalid frame data", "status": "error"})
    except Exception as e:
        print(f"❌ WebSocket error: {e}")
        emit('feedback', {"error": str(e), "status": "error"})

if __name__ == '__main__':
    print("=" * 60)
    print("🤚 Python Processor с MediaPipe запущен")
    print("=" * 60)
    print("📡 Сервер: http://localhost:5001")
    print("📡 WebSocket: ws://localhost:5001")
    print("📁 Отладочные кадры сохраняются в папку 'debug_frames'")
    print("=" * 60)

    # Создаем папку для отладки
    if not os.path.exists('debug_frames'):
        os.makedirs('debug_frames')

    socketio.run(app, host='0.0.0.0', port=5001, debug=True, allow_unsafe_werkzeug=True)