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

# ==================== НАСТРОЙКА ЛОГИРОВАНИЯ ====================
class ColoredTableFormatter(logging.Formatter):
    """Форматтер с цветами и табличным выводом"""

    # Цвета
    grey = "\x1b[38;20m"
    blue = "\x1b[34;20m"
    cyan = "\x1b[36;20m"
    yellow = "\x1b[33;20m"
    red = "\x1b[31;20m"
    bold_red = "\x1b[31;1m"
    green = "\x1b[32;20m"
    reset = "\x1b[0m"

    # Размеры колонок
    LEVEL_WIDTH = 10
    TIME_WIDTH = 23

    def format(self, record):
        # Выбираем цвет для уровня
        level_colors = {
            logging.DEBUG: self.cyan,
            logging.INFO: self.green,
            logging.WARNING: self.yellow,
            logging.ERROR: self.red,
            logging.CRITICAL: self.bold_red
        }
        level_color = level_colors.get(record.levelno, self.grey)

        # Уровень логирования (цветной)
        level_name = f"{record.levelname}".ljust(self.LEVEL_WIDTH - 2)
        level_colored = f"{level_color}[{level_name}]{self.reset}"

        # Время
        timestamp = datetime.fromtimestamp(record.created).strftime('%Y-%m-%d %H:%M:%S')
        timestamp_colored = f"{self.blue}[{timestamp}]{self.reset}"

        # Длительность (если есть)
        duration = ""
        if hasattr(record, 'duration'):
            duration = f"{self.yellow}[{record.duration:>8}ms]{self.reset} "

        # Сообщение
        message = record.getMessage()

        return f"{level_colored} {timestamp_colored} {duration}{message}"

# Настройка корневого логгера
logging.basicConfig(level=logging.INFO)

# Создаем логгер для приложения
logger = logging.getLogger('LFK')
logger.setLevel(logging.DEBUG)

# Убираем стандартные обработчики
logger.handlers.clear()

# Консольный handler с цветами
console_handler = logging.StreamHandler()
console_handler.setLevel(logging.DEBUG)
console_formatter = ColoredTableFormatter()
console_handler.setFormatter(console_formatter)
logger.addHandler(console_handler)

# Файловый handler (без цветов)
file_handler = logging.FileHandler('lfk_python.log')
file_handler.setLevel(logging.INFO)
file_formatter = logging.Formatter(
    '[%(levelname)s] [%(asctime)s] %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
file_handler.setFormatter(file_formatter)
logger.addHandler(file_handler)

# Отключаем лишние логи
logging.getLogger('werkzeug').setLevel(logging.ERROR)
logging.getLogger('socketio').setLevel(logging.ERROR)
logging.getLogger('engineio').setLevel(logging.ERROR)

# Сокращения для удобства
log = logger

# ==================== ИНИЦИАЛИЗАЦИЯ ====================

app = Flask(__name__)
app.config['SECRET_KEY'] = 'secret!'
socketio = SocketIO(app, cors_allowed_origins="*", ping_timeout=60, ping_interval=25, logger=False, engineio_logger=False)

# Инициализация MediaPipe
mp_hands = mp.solutions.hands
mp_pose = mp.solutions.pose
mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles

hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

pose = mp_pose.Pose(
    static_image_mode=False,
    model_complexity=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
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
        log.debug(f"{func.__name__} [{(elapsed/1000):.6f}s]", extra={'duration': f'{elapsed:.3f}ms'})
        return result
    return wrapper

def log_function_call(func):
    """Декоратор для логирования вызова функции"""
    @wraps(func)
    def wrapper(*args, **kwargs):
        log.debug(f"▶ Вызов {func.__name__}")
        result = func(*args, **kwargs)
        log.debug(f"◀ Завершен {func.__name__}")
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
        self.stats = {
            'frames_processed': 0,
            'hands_detected': 0,
            'pose_detected': 0,
            'avg_processing_time': 0
        }

        self.load_exercises()
        self.set_exercise("fist")

        if not os.path.exists('debug_frames'):
            os.makedirs('debug_frames')

        log.info("Менеджер упражнений инициализирован")

    def load_exercises(self):
        """Загружает все упражнения из папки exercises"""
        for ex_id, ex_class in EXERCISE_CLASSES.items():
            try:
                self.exercises[ex_id] = ex_class()
                log.info(f"Загружено упражнение: {ex_id} - {self.exercises[ex_id].name}")
            except Exception as e:
                log.error(f"Ошибка загрузки упражнения {ex_id}: {e}")


    def set_exercise(self, exercise_id):
        """Устанавливает текущее упражнение"""
        if exercise_id in self.exercises:
            self.current_exercise = self.exercises[exercise_id]
            self.current_exercise_id = exercise_id
            log.info(f"Текущее упражнение: {self.current_exercise.name}")
            return True
        else:
            log.error(f"Упражнение {exercise_id} не найдено")
            return False

    def reset_current_exercise(self):
        """Сбрасывает текущее упражнение в начальное состояние"""
        if self.current_exercise and hasattr(self.current_exercise, 'reset'):
            self.current_exercise.reset()
            log.info("Текущее упражнение сброшено")
            return True
        return False

    def reset_exercise_for_new_attempt(self):
        """Сбрасывает текущее упражнение для нового подхода"""
        if self.current_exercise:
            if hasattr(self.current_exercise, 'reset_for_new_attempt'):
                self.current_exercise.reset_for_new_attempt()
                log.info("Упражнение сброшено для нового подхода")
                return True
            elif hasattr(self.current_exercise, 'reset'):
                self.current_exercise.reset()
                log.info("Упражнение сброшено (через reset)")
                return True
        return False

    def get_exercise_list(self):
        """Возвращает список доступных упражнений"""
        return [{"id": ex_id, "name": ex.name} for ex_id, ex in self.exercises.items()]

    def _is_pose_exercise(self):
        """Проверяет, нужно ли использовать pose detection"""
        if hasattr(self.current_exercise, 'body_part'):
            from exercises.base_exercise import BodyPart
            return self.current_exercise.body_part in [BodyPart.POSE, BodyPart.HEAD, BodyPart.SHOULDER]
        return False

    @log_execution_time
    def process_frame(self, frame_data):
        """Обработка кадра"""
        try:
            start_time = time.time()
            self.stats['frames_processed'] += 1

            # Декодируем base64
            if isinstance(frame_data, str):
                try:
                    missing_padding = len(frame_data) % 4
                    if missing_padding:
                        frame_data += '=' * (4 - missing_padding)

                    frame_bytes = base64.b64decode(frame_data)
                    log.debug(f"Декодировано {len(frame_bytes)} байт")
                except Exception as e:
                    log.error(f"Ошибка декодирования base64: {e}")
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
            frame_rgb.flags.writeable = False

            # Создаем копию для визуализации
            display_frame = frame.copy()
            h, w, _ = frame.shape

            # Выбираем детектор в зависимости от упражнения
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

            # Вычисляем и сохраняем время обработки
            process_time = (time.time() - start_time) * 1000
            self.stats['avg_processing_time'] = (
                                                        self.stats['avg_processing_time'] * (self.stats['frames_processed'] - 1) + process_time
                                                ) / self.stats['frames_processed']

            if process_time > 100:
                log.warning(f"Медленная обработка кадра: {process_time:.1f}ms")
            else:
                log.debug(f"Кадр обработан за {(process_time/1000):.6f}s")

            return result

        except Exception as e:
            log.error(f"Критическая ошибка при обработке кадра: {e}")
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

            # Получаем состояние пальцев (старый интерфейс)
            if hasattr(self.current_exercise, 'get_finger_states'):
                finger_states, tip_positions = self.current_exercise.get_finger_states(
                    hand_landmarks, (h, w, 3)
                )
            else:
                finger_states = [False] * 5
                tip_positions = [(0, 0)] * 5

            # Проверяем упражнение
            if hasattr(self.current_exercise, 'check_fingers'):
                is_correct, message = self.current_exercise.check_fingers(
                    finger_states, hand_landmarks, (h, w, 3)
                )
            elif hasattr(self.current_exercise, 'check'):
                landmarks = {'hand': hand_landmarks, 'tip_positions': tip_positions}
                is_correct, message = self.current_exercise.check(landmarks, (h, w, 3))
            else:
                is_correct, message = False, "Неизвестное упражнение"

            # Рисуем обратную связь
            if hasattr(self.current_exercise, 'draw_feedback'):
                display_frame = self.current_exercise.draw_feedback(
                    display_frame, finger_states, tip_positions, is_correct, message
                )

            raised_fingers = sum(finger_states)

            finger_emojis = ['⬆️' if s else '⬇️' for s in finger_states]
            log.debug(f"Состояние пальцев: {finger_emojis}")
            log.debug(f"Результат: {message}")

        return self.success_response(display_frame, True, raised_fingers, finger_states, message)

    def process_pose(self, results, display_frame, h, w):
        """Обрабатывает кадр с позой (оптимизированно)"""
        # Рисуем скелет
        mp_drawing.draw_landmarks(
            display_frame,
            results.pose_landmarks,
            mp_pose.POSE_CONNECTIONS,
            mp_drawing_styles.get_default_pose_landmarks_style()
        )

        # Константы индексов MediaPipe Pose (для скорости)
        NOSE = 0
        LEFT_SHOULDER = 11
        RIGHT_SHOULDER = 12

        pose_landmarks = results.pose_landmarks.landmark

        # Создаем словарь с УДОБНЫМИ СТРОКОВЫМИ КЛЮЧАМИ (именно их ждет NeckExercise)
        landmarks = {
            'nose': pose_landmarks[NOSE],
            'left_shoulder': pose_landmarks[LEFT_SHOULDER],
            'right_shoulder': pose_landmarks[RIGHT_SHOULDER],
            # Добавляем индексы для обратной совместимости
            NOSE: pose_landmarks[NOSE],
            LEFT_SHOULDER: pose_landmarks[LEFT_SHOULDER],
            RIGHT_SHOULDER: pose_landmarks[RIGHT_SHOULDER],
        }

        # Быстрая визуализация для отладки (нос и плечи)
        nose = pose_landmarks[NOSE]
        nx, ny = int(nose.x * w), int(nose.y * h)
        cv2.circle(display_frame, (nx, ny), 8, (0, 255, 255), -1)
        cv2.circle(display_frame, (nx, ny), 8, (255, 255, 255), 2)

        ls = pose_landmarks[LEFT_SHOULDER]
        rs = pose_landmarks[RIGHT_SHOULDER]
        lx, ly = int(ls.x * w), int(ls.y * h)
        rx, ry = int(rs.x * w), int(rs.y * h)
        cv2.circle(display_frame, (lx, ly), 6, (255, 0, 0), -1)
        cv2.circle(display_frame, (rx, ry), 6, (255, 0, 0), -1)
        cv2.line(display_frame, (lx, ly), (rx, ry), (255, 255, 0), 2)

        # Проверяем упражнение
        if hasattr(self.current_exercise, 'check'):
            is_correct, message = self.current_exercise.check(landmarks, (h, w, 3))
        else:
            is_correct, message = False, "Упражнение не поддерживает pose detection"

        # Информационная панель
        cv2.rectangle(display_frame, (5, 5), (450, 130), (0, 0, 0), -1)
        cv2.rectangle(display_frame, (5, 5), (450, 130), (255, 255, 255), 1)

        cv2.putText(display_frame, f"Exercise: {self.current_exercise.name[:25]}", (15, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 1)

        # Показываем статус детекции
        detection_status = "✅ BODY DETECTED" if results.pose_landmarks else "❌ NO BODY"
        cv2.putText(display_frame, detection_status, (15, 55),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

        color = (0, 255, 0) if is_correct else (0, 0, 255)
        msg = message[:45]
        cv2.putText(display_frame, msg, (15, 80),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.55, color, 1)

        # Статус калибровки
        if hasattr(self.current_exercise, 'calibrated'):
            calib_text = "✅ CALIBRATED" if self.current_exercise.calibrated else "⚠️ CALIBRATING..."
            calib_color = (0, 255, 0) if self.current_exercise.calibrated else (0, 255, 255)
            cv2.putText(display_frame, calib_text, (15, 105),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, calib_color, 1)

        return self.success_response(display_frame, True, 0, [False]*5, message)

    def no_hand_response(self, display_frame):
        """Ответ когда нет руки"""
        cv2.rectangle(display_frame, (5, 5), (200, 50), (0, 0, 0), -1)
        cv2.putText(display_frame, "NO HAND", (15, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        log.debug("Рука не обнаружена")
        return self.success_response(display_frame, False, 0, [False]*5, "Рука не обнаружена")

    def no_pose_response(self, display_frame):
        """Ответ когда нет тела"""
        cv2.rectangle(display_frame, (5, 5), (200, 50), (0, 0, 0), -1)
        cv2.putText(display_frame, "NO BODY", (15, 35),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
        log.debug("Тело не обнаружено")
        return self.success_response(display_frame, False, 0, [False]*5, "Тело не обнаружено")

    def success_response(self, frame, detected, raised, states, message):
        """Формирует успешный ответ"""
        try:
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 70])
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

    def error_response(self, message):
        """Формирует ответ с ошибкой"""
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
        """Выводит статистику работы"""
        log.info("=" * 60)
        log.info("СТАТИСТИКА РАБОТЫ:")
        log.info(f"  Обработано кадров: {self.stats['frames_processed']}")
        log.info(f"  Рук обнаружено: {self.stats['hands_detected']}")
        log.info(f"  Поз обнаружено: {self.stats['pose_detected']}")
        log.info(f"  Среднее время обработки: {self.stats['avg_processing_time']:.1f}ms")
        log.info("=" * 60)

# Создаем менеджер упражнений
exercise_manager = ExerciseManager()

# ==================== МАРШРУТЫ ====================

@app.route('/health', methods=['GET'])
def health():
    """Проверка здоровья сервиса"""
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
    """Возвращает список доступных упражнений"""
    return jsonify({
        "exercises": exercise_manager.get_exercise_list()
    })

@app.route('/stats', methods=['GET'])
def get_stats():
    """Возвращает статистику работы"""
    return jsonify(exercise_manager.stats)

@app.route('/exercise_state', methods=['GET'])
def get_exercise_state():
    """Возвращает текущее состояние упражнения"""
    try:
        exercise_type = request.args.get('type', 'fist-palm')

        if exercise_type != exercise_manager.current_exercise_id:
            exercise_manager.set_exercise(exercise_type)

        structured = None
        if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
            structured = exercise_manager.current_exercise.get_structured_data()

        log.info(f"Запрос состояния: {exercise_type}")
        return jsonify({
            "status": "success",
            "current_exercise": exercise_manager.current_exercise_id,
            "exercise_name": exercise_manager.current_exercise.name,
            "structured": structured,
            "auto_reset": getattr(exercise_manager.current_exercise, 'auto_reset_on_next_start', False)
        })
    except Exception as e:
        log.error(f"Ошибка получения состояния: {e}")
        return jsonify({"error": str(e), "status": "error"}), 500

@app.route('/reset_exercise', methods=['POST'])
def reset_exercise():
    """Сбрасывает текущее упражнение (только по запросу)"""
    try:
        if exercise_manager.reset_current_exercise():
            log.info("Упражнение сброшено по запросу")
            return jsonify({
                "status": "success",
                "message": "Exercise reset successfully"
            })
        else:
            return jsonify({
                "status": "error",
                "message": "Exercise does not support reset"
            }), 400
    except Exception as e:
        log.error(f"Ошибка при сбросе: {e}")
        return jsonify({"error": str(e), "status": "error"}), 400

@app.route('/reset_for_new_attempt', methods=['POST'])
def reset_for_new_attempt():
    """Сбрасывает текущее упражнение для нового подхода"""
    try:
        if exercise_manager.reset_exercise_for_new_attempt():
            log.info("Упражнение сброшено для нового подхода")
            return jsonify({
                "status": "success",
                "message": "Exercise reset for new attempt"
            })
        else:
            return jsonify({
                "status": "error",
                "message": "Exercise does not support reset"
            }), 400
    except Exception as e:
        log.error(f"Ошибка при сбросе: {e}")
        return jsonify({"error": str(e), "status": "error"}), 400

@app.route('/set_exercise', methods=['POST'])
def set_exercise():
    """Смена упражнения"""
    try:
        data = request.get_json()
        exercise_id = data.get('exercise_id')

        if exercise_manager.set_exercise(exercise_id):
            log.info(f"Смена упражнения на: {exercise_id}")
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
        log.error(f"Ошибка при смене упражнения: {e}")
        return jsonify({"error": str(e), "status": "error"}), 400

@app.route('/process', methods=['POST'])
def process_frame():
    """Обработка кадра"""
    try:
        data = request.get_json()
        if not data:
            log.warning("Пустой запрос")
            return jsonify({"error": "No data provided"}), 400

        # Проверяем, есть ли запрос только на получение состояния
        if data.get('get_state_only'):
            exercise_type = data.get('exercise_type', 'fist-palm')
            log.info(f"Запрос состояния: {exercise_type}")

            if exercise_type != exercise_manager.current_exercise_id:
                exercise_manager.set_exercise(exercise_type)

            structured = None
            if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
                structured = exercise_manager.current_exercise.get_structured_data()

            return jsonify({
                "status": "success",
                "current_exercise": exercise_manager.current_exercise_id,
                "exercise_name": exercise_manager.current_exercise.name if exercise_manager.current_exercise else "unknown",
                "hand_detected": False,
                "raised_fingers": 0,
                "finger_states": [False]*5,
                "structured": structured,
                "message": "State check"
            })

        # Проверяем, есть ли запрос на сброс для нового подхода
        if data.get('reset_for_new_attempt'):
            exercise_type = data.get('exercise_type', 'fist-palm')
            log.info(f"Сброс упражнения: {exercise_type}")

            if exercise_type != exercise_manager.current_exercise_id:
                exercise_manager.set_exercise(exercise_type)

            success = exercise_manager.reset_exercise_for_new_attempt()

            if success:
                structured = None
                if hasattr(exercise_manager.current_exercise, 'get_structured_data'):
                    structured = exercise_manager.current_exercise.get_structured_data()

                return jsonify({
                    "status": "success",
                    "message": "Exercise reset successfully",
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "finger_states": [False]*5,
                    "current_exercise": exercise_manager.current_exercise_id,
                    "exercise_name": exercise_manager.current_exercise.name if exercise_manager.current_exercise else "unknown",
                    "structured": structured
                })
            else:
                return jsonify({
                    "status": "error",
                    "message": "Failed to reset exercise",
                    "error": "Exercise does not support reset"
                }), 400

        # Проверяем, есть ли смена упражнения
        if 'exercise_type' in data:
            exercise_manager.set_exercise(data['exercise_type'])

        frame = data.get('frame')
        if not frame:
            log.warning("Нет данных кадра")
            return jsonify({"error": "No frame provided"}), 400

        log.info(f"Получен кадр, размер: {len(frame)} байт")
        result = exercise_manager.process_frame(frame)
        log.info(f"Кадр обработан, hand_detected={result.get('hand_detected')}")

        # Если упражнение завершено, помечаем его для сброса при следующем запуске
        if result and result.get('structured') and result['structured'].get('completed'):
            log.info("Упражнение завершено")
            if hasattr(exercise_manager.current_exercise, 'mark_for_reset'):
                exercise_manager.current_exercise.mark_for_reset()

        return jsonify(result)
    except Exception as e:
        log.error(f"Ошибка при обработке: {e}")
        import traceback
        traceback.print_exc()
        return jsonify({
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"Server error: {str(e)}",
            "processed_frame": "",
            "current_exercise": exercise_manager.current_exercise_id,
            "status": "error"
        }), 500

# ==================== WEBSOCKET ====================

@socketio.on('connect')
def handle_connect():
    global frame_counter
    log.info(f"Клиент подключен, SID: {request.sid}")
    frame_counter = 0
    frame_buffer.clear()

    if hasattr(exercise_manager.current_exercise, 'auto_reset_on_next_start') and exercise_manager.current_exercise.auto_reset_on_next_start:
        log.info("Автоматический сброс упражнения при новом подключении")
        exercise_manager.reset_exercise_for_new_attempt()
    else:
        exercise_manager.reset_current_exercise()

@socketio.on('disconnect')
def handle_disconnect():
    log.info(f"Клиент отключен, SID: {request.sid}")

@socketio.on('frame')
def handle_frame(data):
    try:
        start_time = time.time()

        if isinstance(data, dict):
            if 'exercise_type' in data:
                exercise_manager.set_exercise(data['exercise_type'])

            frame = data.get('frame')
            if frame:
                result = exercise_manager.process_frame(frame)
                emit('feedback', result)

                process_time = (time.time() - start_time) * 1000
                log.debug(f"WebSocket обработка: {process_time:.1f}ms")

                if result and result.get('structured') and result['structured'].get('completed'):
                    log.info("Упражнение завершено и помечено для автосброса")
            else:
                emit('feedback', {
                    "hand_detected": False,
                    "raised_fingers": 0,
                    "message": "No frame data",
                    "processed_frame": "",
                    "current_exercise": exercise_manager.current_exercise_id,
                    "status": "error"
                })
        else:
            emit('feedback', {
                "hand_detected": False,
                "raised_fingers": 0,
                "message": "Invalid data format",
                "processed_frame": "",
                "current_exercise": exercise_manager.current_exercise_id,
                "status": "error"
            })
    except Exception as e:
        log.error(f"WebSocket ошибка: {e}")
        emit('feedback', {
            "hand_detected": False,
            "raised_fingers": 0,
            "message": f"WebSocket error: {str(e)}",
            "processed_frame": "",
            "current_exercise": exercise_manager.current_exercise_id,
            "status": "error"
        })

# ==================== ЗАПУСК ====================

if __name__ == '__main__':
    print("\n" + "=" * 60)
    print("🤚 PYTHON PROCESSOR".center(58))
    print("=" * 60)
    print(f"📡 Сервер: http://localhost:5001")
    print(f"📁 Лог-файл: lfk_python.log")
    print("\n📋 Доступные упражнения:")
    for ex in exercise_manager.get_exercise_list():
        print(f"   • {ex['id']}: {ex['name']}")
    print("\n" + "=" * 60 + "\n")

    # Периодический вывод статистики
    import threading
    def stats_reporter():
        while True:
            time.sleep(60)  # Каждую минуту
            exercise_manager.print_stats()

    threading.Thread(target=stats_reporter, daemon=True).start()

    socketio.run(app, host='0.0.0.0', port=5001, debug=False, allow_unsafe_werkzeug=True)