import cv2
import mediapipe as mp
import base64
import numpy as np
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import logging
import time
import os
from collections import deque
from functools import wraps
from datetime import datetime

# Импортируем упражнения
from exercises import EXERCISE_CLASSES

# ==================== ОПТИМИЗАЦИЯ ====================
# Отключаем ненужные логи MediaPipe
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3'
os.environ['MEDIAPIPE_DISABLE_GPU'] = '1'

# ==================== НАСТРОЙКА ЛОГИРОВАНИЯ ====================
class ColoredTableFormatter(logging.Formatter):
    """Форматтер с цветами и табличным выводом"""
    grey = "\x1b[38;20m"
    blue = "\x1b[34;20m"
    cyan = "\x1b[36;20m"
    yellow = "\x1b[33;20m"
    red = "\x1b[31;20m"
    bold_red = "\x1b[31;1m"
    green = "\x1b[32;20m"
    reset = "\x1b[0m"

    LEVEL_WIDTH = 10
    TIME_WIDTH = 23

    def format(self, record):
        level_colors = {
            logging.DEBUG: self.cyan,
            logging.INFO: self.green,
            logging.WARNING: self.yellow,
            logging.ERROR: self.red,
            logging.CRITICAL: self.bold_red
        }
        level_color = level_colors.get(record.levelno, self.grey)
        level_name = f"{record.levelname}".ljust(self.LEVEL_WIDTH - 2)
        level_colored = f"{level_color}[{level_name}]{self.reset}"
        timestamp = datetime.fromtimestamp(record.created).strftime('%Y-%m-%d %H:%M:%S')
        timestamp_colored = f"{self.blue}[{timestamp}]{self.reset}"
        duration = ""
        if hasattr(record, 'duration'):
            duration = f"{self.yellow}[{record.duration:>8}ms]{self.reset} "
        message = record.getMessage()
        return f"{level_colored} {timestamp_colored} {duration}{message}"

# Настройка логгера
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('LFK')
logger.setLevel(logging.INFO)  # INFO вместо DEBUG для скорости

logger.handlers.clear()
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.INFO)
console_formatter = ColoredTableFormatter()
console_handler.setFormatter(console_formatter)
logger.addHandler(console_handler)

# Файловый логгер - отключаем для скорости на HDD
# file_handler = logging.FileHandler('lfk_python.log')
# file_handler.setLevel(logging.INFO)
# file_formatter = logging.Formatter('[%(levelname)s] [%(asctime)s] %(message)s', datefmt='%Y-%m-%d %H:%M:%S')
# file_handler.setFormatter(file_formatter)
# logger.addHandler(file_handler)

# Отключаем лишние логи
logging.getLogger('werkzeug').setLevel(logging.ERROR)
logging.getLogger('socketio').setLevel(logging.ERROR)
logging.getLogger('engineio').setLevel(logging.ERROR)
logging.getLogger('mediapipe').setLevel(logging.ERROR)

log = logger

# ==================== КОНСТАНТЫ ДЛЯ ОПТИМИЗАЦИИ ====================
JPEG_QUALITY = 60  # Снижаем качество с 70 до 60
FRAME_PROCESS_INTERVAL = 2  # Обрабатываем каждый 2-й кадр
DETECTION_CONFIDENCE = 0.4  # Снижаем порог для скорости

# ==================== ИНИЦИАЛИЗАЦИЯ ====================
app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*", ping_timeout=60, ping_interval=25,
                    logger=False, engineio_logger=False)

mp_hands = mp.solutions.hands
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles

# Оптимизированные параметры MediaPipe
hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=DETECTION_CONFIDENCE,
    min_tracking_confidence=DETECTION_CONFIDENCE,
    model_complexity=0  # Используем самую простую модель
)

pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=0,  # Самая простая модель
    smooth_landmarks=False,  # Отключаем сглаживание
    min_detection_confidence=DETECTION_CONFIDENCE,
    min_tracking_confidence=DETECTION_CONFIDENCE
)

frame_counter = 0
frame_buffer = deque(maxlen=2)
last_processed_time = time.time()

# ==================== ДЕКОРАТОРЫ ====================
def log_execution_time(func):
    """Декоратор для измерения времени выполнения"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        start = time.perf_counter()
        result = func(*args, **kwargs)
        elapsed = (time.perf_counter() - start) * 1000
        if elapsed > 100:  # Логируем только медленные операции
            log.warning(f"SLOW: {func.__name__} {elapsed:.1f}ms")
        return result
    return wrapper

# ==================== МЕНЕДЖЕР УПРАЖНЕНИЙ ====================
class ExerciseManager:
    """Менеджер упражнений"""

    def __init__(self):
        self.exercises = {}
        self.current_exercise = None
        self.current_exercise_id = "fist"
        self.connection_count = 0
        self.frame_skip_counter = 0
        self.stats = {
            'frames_processed': 0,
            'hands_detected': 0,
            'pose_detected': 0,
            'avg_processing_time': 0,
            'frames_skipped': 0
        }

        self.load_exercises()
        self.set_exercise("fist")
        log.info("Менеджер упражнений инициализирован")

    def load_exercises(self):
        for ex_id, ex_class in EXERCISE_CLASSES.items():
            try:
                self.exercises[ex_id] = ex_class()
                log.info(f"Загружено: {ex_id} - {self.exercises[ex_id].name}")
            except Exception as e:
                log.error(f"Ошибка загрузки {ex_id}: {e}")

    def set_exercise(self, exercise_id):
        if exercise_id in self.exercises:
            self.current_exercise = self.exercises[exercise_id]
            self.current_exercise_id = exercise_id
            log.info(f"Текущее упражнение: {self.current_exercise.name}")
            return True
        else:
            log.error(f"Упражнение {exercise_id} не найдено")
            return False

    def reset_current_exercise(self):
        if self.current_exercise and hasattr(self.current_exercise, 'reset'):
            self.current_exercise.reset()
            log.info("Упражнение сброшено")
            return True
        return False

    def reset_exercise_for_new_attempt(self):
        if self.current_exercise:
            if hasattr(self.current_exercise, 'reset_for_new_attempt'):
                self.current_exercise.reset_for_new_attempt()
            elif hasattr(self.current_exercise, 'reset'):
                self.current_exercise.reset()
            log.info("Упражнение сброшено для нового подхода")
            return True
        return False

    def get_exercise_list(self):
        return [{"id": ex_id, "name": ex.name} for ex_id, ex in self.exercises.items()]

    def _is_pose_exercise(self):
        if hasattr(self.current_exercise, 'body_part'):
            from exercises.base_exercise import BodyPart
            return self.current_exercise.body_part in [BodyPart.POSE, BodyPart.HEAD, BodyPart.SHOULDER]
        return False

    @log_execution_time
    def process_frame(self, frame_data):
        """Обработка кадра с пропуском кадров"""
        self.frame_skip_counter += 1

        # Пропускаем каждый 2-й кадр
        if self.frame_skip_counter < FRAME_PROCESS_INTERVAL:
            self.stats['frames_skipped'] += 1
            return self._skip_response()
        self.frame_skip_counter = 0

        start_time = time.time()
        self.stats['frames_processed'] += 1

        try:
            # Декодируем base64
            if isinstance(frame_data, str):
                try:
                    missing_padding = len(frame_data) % 4
                    if missing_padding:
                        frame_data += '=' * (4 - missing_padding)
                    frame_bytes = base64.b64decode(frame_data)
                except Exception as e:
                    log.error(f"Ошибка декодирования: {e}")
                    return self.error_response("Ошибка декодирования")
            else:
                return self.error_response("Invalid frame data type")

            nparr = np.frombuffer(frame_bytes, np.uint8)
            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

            if frame is None:
                return self.error_response("Cannot decode image")

            frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            frame_rgb.flags.writeable = False

            display_frame = frame.copy()
            h, w, _ = frame.shape

            if self._is_pose_exercise():
                results = pose.process(frame_rgb)
                frame_rgb.flags.writeable = True
                if results.pose_landmarks:
                    self.stats['pose_detected'] += 1
                    result = self.process_pose(results, display_frame, h, w)
                else:
                    result = self.no_pose_response(display_frame)
            else:
                results = hands.process(frame_rgb)
                frame_rgb.flags.writeable = True
                if results.multi_hand_landmarks:
                    self.stats['hands_detected'] += 1
                    result = self.process_hand(results, display_frame, h, w)
                else:
                    result = self.no_hand_response(display_frame)

            process_time = (time.time() - start_time) * 1000
            self.stats['avg_processing_time'] = (
                                                        self.stats['avg_processing_time'] * (self.stats['frames_processed'] - 1) + process_time
                                                ) / self.stats['frames_processed']

            return result

        except Exception as e:
            log.error(f"Критическая ошибка: {e}")
            import traceback
            traceback.print_exc()
            return self.error_response(str(e))

    def process_hand(self, results, display_frame, h, w):
        """Обрабатывает кадр с рукой"""
        raised_fingers = 0
        finger_states = []

        for hand_landmarks in results.multi_hand_landmarks:
            # Рисуем скелет (упрощенно для скорости)
            mp_drawing.draw_landmarks(
                display_frame, hand_landmarks, mp_hands.HAND_CONNECTIONS,
                mp_drawing_styles.get_default_hand_landmarks_style(),
                mp_drawing_styles.get_default_hand_connections_style()
            )

            if hasattr(self.current_exercise, 'get_finger_states'):
                finger_states, tip_positions = self.current_exercise.get_finger_states(
                    hand_landmarks, (h, w, 3)
                )
            else:
                finger_states = [False] * 5
                tip_positions = [(0, 0)] * 5

            if hasattr(self.current_exercise, 'check_fingers'):
                is_correct, message = self.current_exercise.check_fingers(
                    finger_states, hand_landmarks, (h, w, 3)
                )
            elif hasattr(self.current_exercise, 'check'):
                landmarks = {'hand': hand_landmarks, 'tip_positions': tip_positions}
                is_correct, message = self.current_exercise.check(landmarks, (h, w, 3))
            else:
                is_correct, message = False, "Неизвестное упражнение"

            if hasattr(self.current_exercise, 'draw_feedback'):
                display_frame = self.current_exercise.draw_feedback(
                    display_frame, finger_states, tip_positions, is_correct, message
                )

            raised_fingers = sum(finger_states)

        return self.success_response(display_frame, True, raised_fingers, finger_states, message)

    def process_pose(self, results, display_frame, h, w):
        """Обрабатывает кадр с позой"""
        mp_drawing.draw_landmarks(
            display_frame, results.pose_landmarks, mp_pose.POSE_CONNECTIONS,
            mp_drawing_styles.get_default_pose_landmarks_style()
        )

        NOSE, LEFT_SHOULDER, RIGHT_SHOULDER = 0, 11, 12
        pose_landmarks = results.pose_landmarks.landmark

        landmarks = {
            'nose': pose_landmarks[NOSE],
            'left_shoulder': pose_landmarks[LEFT_SHOULDER],
            'right_shoulder': pose_landmarks[RIGHT_SHOULDER],
            NOSE: pose_landmarks[NOSE],
            LEFT_SHOULDER: pose_landmarks[LEFT_SHOULDER],
            RIGHT_SHOULDER: pose_landmarks[RIGHT_SHOULDER],
        }

        # Визуализация (упрощенная)
        nose = pose_landmarks[NOSE]
        nx, ny = int(nose.x * w), int(nose.y * h)
        cv2.circle(display_frame, (nx, ny), 6, (0, 255, 255), -1)

        ls = pose_landmarks[LEFT_SHOULDER]
        rs = pose_landmarks[RIGHT_SHOULDER]
        lx, ly = int(ls.x * w), int(ls.y * h)
        rx, ry = int(rs.x * w), int(rs.y * h)
        cv2.circle(display_frame, (lx, ly), 5, (255, 0, 0), -1)
        cv2.circle(display_frame, (rx, ry), 5, (255, 0, 0), -1)
        cv2.line(display_frame, (lx, ly), (rx, ry), (255, 255, 0), 2)

        if hasattr(self.current_exercise, 'check'):
            is_correct, message = self.current_exercise.check(landmarks, (h, w, 3))
        else:
            is_correct, message = False, "Упражнение не поддерживает pose detection"

        # Информационная панель (упрощенная)
        cv2.rectangle(display_frame, (5, 5), (400, 100), (0, 0, 0), -1)
        cv2.putText(display_frame, f"{self.current_exercise.name[:20]}", (15, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1)

        color = (0, 255, 0) if is_correct else (0, 0, 255)
        cv2.putText(display_frame, message[:40], (15, 60),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 1)

        if hasattr(self.current_exercise, 'calibrated'):
            calib_text = "CALIBRATED" if self.current_exercise.calibrated else "CALIBRATING..."
            calib_color = (0, 255, 0) if self.current_exercise.calibrated else (0, 255, 255)
            cv2.putText(display_frame, calib_text, (15, 85),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, calib_color, 1)

        return self.success_response(display_frame, True, 0, [False]*5, message)

    def no_hand_response(self, display_frame):
        cv2.rectangle(display_frame, (5, 5), (180, 45), (0, 0, 0), -1)
        cv2.putText(display_frame, "NO HAND", (15, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
        return self.success_response(display_frame, False, 0, [False]*5, "Рука не обнаружена")

    def no_pose_response(self, display_frame):
        cv2.rectangle(display_frame, (5, 5), (180, 45), (0, 0, 0), -1)
        cv2.putText(display_frame, "NO BODY", (15, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, (0, 0, 255), 2)
        return self.success_response(display_frame, False, 0, [False]*5, "Тело не обнаружено")

    def success_response(self, frame, detected, raised, states, message):
        try:
            # Сниженное качество JPEG
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY])
            frame_out = base64.b64encode(buffer).decode('utf-8')

            response = {
                "hand_detected": detected,
                "raised_fingers": raised,
                "finger_states": states,
                "message": message,
                "processed_frame": frame_out,
                "current_exercise": self.current_exercise_id,
                "exercise_name": self.current_exercise.name,
                "status": "success"
            }

            if hasattr(self.current_exercise, 'get_structured_data'):
                structured = self.current_exercise.get_structured_data()
                if structured:
                    response["structured"] = structured

            return response
        except Exception as e:
            log.error(f"Ошибка при формировании ответа: {e}")
            return self.error_response("Error creating response")

    def _skip_response(self):
        """Ответ при пропуске кадра"""
        return {
            "hand_detected": False,
            "raised_fingers": 0,
            "finger_states": [False]*5,
            "message": "",
            "processed_frame": "",
            "current_exercise": self.current_exercise_id,
            "exercise_name": self.current_exercise.name if self.current_exercise else "unknown",
            "status": "skipped"
        }

    def error_response(self, message):
        return {
            "hand_detected": False,
            "raised_fingers": 0,
            "finger_states": [False]*5,
            "message": message,
            "processed_frame": "",
            "current_exercise": self.current_exercise_id,
            "exercise_name": self.current_exercise.name if self.current_exercise else "unknown",
            "status": "error"
        }

    def print_stats(self):
        log.info("=" * 60)
        log.info("СТАТИСТИКА РАБОТЫ:")
        log.info(f"  Обработано кадров: {self.stats['frames_processed']}")
        log.info(f"  Пропущено кадров: {self.stats['frames_skipped']}")
        log.info(f"  Рук обнаружено: {self.stats['hands_detected']}")
        log.info(f"  Поз обнаружено: {self.stats['pose_detected']}")
        log.info(f"  Среднее время: {self.stats['avg_processing_time']:.1f}ms")
        log.info("=" * 60)


exercise_manager = ExerciseManager()

# ==================== МАРШРУТЫ ====================
@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        "status": "ok",
        "current_exercise": exercise_manager.current_exercise_id,
        "available_exercises": exercise_manager.get_exercise_list(),
        "stats": {
            "frames_processed": exercise_manager.stats['frames_processed'],
            "avg_processing_time": round(exercise_manager.stats['avg_processing_time'], 1)
        }
    })

@app.route('/exercises', methods=['GET'])
def list_exercises():
    return jsonify({"exercises": exercise_manager.get_exercise_list()})

@app.route('/stats', methods=['GET'])
def get_stats():
    return jsonify(exercise_manager.stats)

@app.route('/exercise_state', methods=['GET'])
def get_exercise_state():
    try:
        exercise_type = request.args.get('type', 'fist-palm')
        if exercise_type != exercise_manager.current_exercise_id:
            exercise_manager.set_exercise(exercise_type)

        structured = None
        if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
            structured = exercise_manager.current_exercise.get_structured_data()

        return jsonify({
            "status": "success",
            "current_exercise": exercise_manager.current_exercise_id,
            "exercise_name": exercise_manager.current_exercise.name,
            "structured": structured,
            "auto_reset": getattr(exercise_manager.current_exercise, 'auto_reset_on_next_start', False)
        })
    except Exception as e:
        return jsonify({"error": str(e), "status": "error"}), 500

@app.route('/reset_exercise', methods=['POST'])
def reset_exercise():
    if exercise_manager.reset_current_exercise():
        return jsonify({"status": "success", "message": "Exercise reset successfully"})
    return jsonify({"status": "error", "message": "Exercise does not support reset"}), 400

@app.route('/reset_for_new_attempt', methods=['POST'])
def reset_for_new_attempt():
    if exercise_manager.reset_exercise_for_new_attempt():
        return jsonify({"status": "success", "message": "Exercise reset for new attempt"})
    return jsonify({"status": "error", "message": "Exercise does not support reset"}), 400

@app.route('/set_exercise', methods=['POST'])
def set_exercise():
    data = request.get_json()
    if exercise_manager.set_exercise(data.get('exercise_id')):
        return jsonify({"status": "success", "current_exercise": exercise_manager.current_exercise_id})
    return jsonify({"status": "error", "message": "Exercise not found"}), 400

@app.route('/process', methods=['POST'])
def process_frame():
    try:
        data = request.get_json()
        if not data:
            return jsonify({"error": "No data provided"}), 400

        if data.get('get_state_only'):
            exercise_type = data.get('exercise_type', 'fist-palm')
            if exercise_type != exercise_manager.current_exercise_id:
                exercise_manager.set_exercise(exercise_type)
            structured = None
            if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
                structured = exercise_manager.current_exercise.get_structured_data()
            return jsonify({
                "status": "success",
                "current_exercise": exercise_manager.current_exercise_id,
                "structured": structured,
                "message": "State check"
            })

        if data.get('reset_for_new_attempt'):
            exercise_type = data.get('exercise_type', 'fist-palm')
            if exercise_type != exercise_manager.current_exercise_id:
                exercise_manager.set_exercise(exercise_type)
            success = exercise_manager.reset_exercise_for_new_attempt()
            if success:
                structured = None
                if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
                    structured = exercise_manager.current_exercise.get_structured_data()
                return jsonify({"status": "success", "structured": structured})
            return jsonify({"status": "error"}), 400

        if 'exercise_type' in data:
            exercise_manager.set_exercise(data['exercise_type'])

        frame = data.get('frame')
        if not frame:
            return jsonify({"error": "No frame provided"}), 400

        result = exercise_manager.process_frame(frame)

        if result and result.get('structured') and result['structured'].get('completed'):
            if hasattr(exercise_manager.current_exercise, 'mark_for_reset'):
                exercise_manager.current_exercise.mark_for_reset()

        return jsonify(result)
    except Exception as e:
        log.error(f"Ошибка при обработке: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({"status": "error", "message": str(e)}), 500

# ==================== WEBSOCKET ====================
@socketio.on('connect')
def handle_connect():
    global frame_counter
    log.info(f"Клиент подключен: {request.sid}")
    frame_counter = 0
    frame_buffer.clear()

    if hasattr(exercise_manager.current_exercise, 'auto_reset_on_next_start') and exercise_manager.current_exercise.auto_reset_on_next_start:
        exercise_manager.reset_exercise_for_new_attempt()
    else:
        exercise_manager.reset_current_exercise()

@socketio.on('disconnect')
def handle_disconnect():
    log.info(f"Клиент отключен: {request.sid}")

@socketio.on('frame')
def handle_frame(data):
    try:
        if isinstance(data, dict):
            if 'exercise_type' in data:
                exercise_manager.set_exercise(data['exercise_type'])
            frame = data.get('frame')
            if frame:
                result = exercise_manager.process_frame(frame)
                emit('feedback', result)
                if result and result.get('structured') and result['structured'].get('completed'):
                    log.info("Упражнение завершено")
            else:
                emit('feedback', {"status": "error", "message": "No frame data"})
        else:
            emit('feedback', {"status": "error", "message": "Invalid data format"})
    except Exception as e:
        log.error(f"WebSocket ошибка: {e}")
        emit('feedback', {"status": "error", "message": str(e)})

# ==================== ЗАПУСК ====================
if __name__ == '__main__':
    print("\n" + "=" * 60)
    print("🤚 PYTHON PROCESSOR".center(58))
    print("=" * 60)
    print(f"📡 Сервер: http://localhost:5001")
    print(f"🎯 Quality: {JPEG_QUALITY}%")
    print(f"⏩ Frame skip: каждый {FRAME_PROCESS_INTERVAL}-й кадр")
    print("\n📋 Доступные упражнения:")
    for ex in exercise_manager.get_exercise_list():
        print(f"   • {ex['id']}: {ex['name']}")
    print("\n" + "=" * 60 + "\n")

    import threading
    def stats_reporter():
        while True:
            time.sleep(60)
            exercise_manager.print_stats()

    threading.Thread(target=stats_reporter, daemon=True).start()
    socketio.run(app, host='0.0.0.0', port=5001, debug=False, allow_unsafe_werkzeug=True)