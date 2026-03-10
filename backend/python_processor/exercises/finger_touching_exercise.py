"""
Упражнение "Считалочка" - поочередное касание пальцев с большим
"""

import time
import math
import logging
from .base_exercise import BaseExercise

# Настройка логгера для этого упражнения
logger = logging.getLogger('LKF.Exercise.FingerTouching')

class FingerTouchingExercise(BaseExercise):
    """Упражнение: Поочередное касание пальцев с большим"""

    # Константы для состояний
    STATE_WAITING = "waiting"
    STATE_TOUCHING = "touching"
    STATE_COMPLETED = "completed"

    # Названия пальцев
    FINGER_NAMES = ["большой", "указательный", "средний", "безымянный", "мизинец"]
    TARGET_FINGER_NAMES = ["указательным", "средним", "безымянным", "мизинцем"]

    def __init__(self):
        super().__init__()
        self.name = "Считалочка"
        self.description = "Поочередное касание пальцев - развивает мелкую моторику"
        self.exercise_id = "finger-touching"

        # Состояние упражнения
        self.state = self.STATE_WAITING
        self.state_start_time = time.time()

        # Счетчики
        self.current_cycle = 0
        self.total_cycles = 5  # 5 полных циклов (по 4 касания в каждом)
        self.current_finger = 0  # 0=указательный, 1=средний, 2=безымянный, 3=мизинец
        self.touches_in_cycle = 0  # количество касаний в текущем цикле (0-4)

        # Пороговое расстояние для касания (в относительных координатах)
        self.touch_threshold = 0.05  # 5% от размера кадра

        # Тайминги
        self.hold_duration = 1.0  # секунд удержания касания
        self.countdown = None
        self.last_countdown_update = time.time()

        # Флаги
        self.current_touch_start_time = None
        self.completed_flag = False
        self.auto_reset_on_next_start = False

        # Для отслеживания последнего успешного касания
        self.last_touch_time = 0
        self.touch_cooldown = 0.3  # задержка между касаниями

        # Структурированные данные для клиента
        self.structured_data = self._get_structured_data()

        logger.info(f"Упражнение инициализировано: {self.name}")
        logger.debug(f"Параметры: циклов={self.total_cycles}, удержание={self.hold_duration}с, порог={self.touch_threshold}")

    def reset(self):
        """Сбрасывает упражнение в начальное состояние"""
        self.state = self.STATE_WAITING
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.current_finger = 0
        self.touches_in_cycle = 0
        self.countdown = None
        self.current_touch_start_time = None
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.last_touch_time = 0
        self.structured_data = self._get_structured_data()

        logger.info("Упражнение сброшено в начальное состояние")
        return True

    def reset_for_new_attempt(self):
        """Сбрасывает упражнение для нового подхода"""
        self.reset()
        logger.info("Упражнение сброшено для нового подхода")
        return True

    def mark_for_reset(self):
        """Помечает упражнение для сброса при следующем запуске"""
        self.auto_reset_on_next_start = True
        logger.info("Упражнение помечено для автосброса при следующем запуске")

    def check_and_reset_if_needed(self):
        """Проверяет, нужно ли сбросить упражнение"""
        if self.auto_reset_on_next_start:
            logger.debug("Автоматический сброс при новом запуске")
            self.reset_for_new_attempt()
            return True
        return False

    def _get_finger_name(self, finger_index):
        """Возвращает название пальца по индексу"""
        if 0 <= finger_index < len(self.TARGET_FINGER_NAMES):
            return self.TARGET_FINGER_NAMES[finger_index]
        return "неизвестным"

    def _get_state_name(self):
        """Возвращает название текущего состояния"""
        state_names = {
            self.STATE_WAITING: "Ожидание касания",
            self.STATE_TOUCHING: f"Касание с {self._get_finger_name(self.current_finger)}",
            self.STATE_COMPLETED: "Упражнение завершено"
        }
        return state_names.get(self.state, "Неизвестно")

    def _get_progress_percent(self):
        """Возвращает общий прогресс в процентах"""
        total_done = self.current_cycle * 4 + self.touches_in_cycle
        total_needed = self.total_cycles * 4
        return int((total_done / total_needed) * 100) if total_needed > 0 else 0

    def _get_state_message(self):
        """Возвращает сообщение для текущего состояния"""
        if self.state == self.STATE_COMPLETED:
            return f"Упражнение завершено! Выполнено {self.total_cycles} циклов"

        progress = self._get_progress_percent()
        next_finger = self.current_finger
        finger_name = self._get_finger_name(next_finger)
        cycle_display = self.current_cycle + 1

        if self.state == self.STATE_WAITING:
            if self.touches_in_cycle == 0 and self.current_cycle == 0:
                return f"Коснитесь большим пальцем указательного (цикл {cycle_display}/{self.total_cycles}) [{progress}%]"
            else:
                return f"Коснитесь большим пальцем с {finger_name} (цикл {cycle_display}/{self.total_cycles}) [{progress}%]"

        elif self.state == self.STATE_TOUCHING:
            remaining = int(self.hold_duration - (time.time() - self.current_touch_start_time)) + 1
            return f"Держите касание с {finger_name}... {remaining}с ({progress}%)"

        return ""

    def _get_structured_data(self):
        """Формирует структурированные данные для клиента"""
        data = {
            "state": self.state,
            "state_name": self._get_state_name(),
            "current_cycle": self.current_cycle,
            "total_cycles": self.total_cycles,
            "current_finger": self.current_finger,
            "touches_in_cycle": self.touches_in_cycle,
            "countdown": self.countdown,
            "progress_percent": self._get_progress_percent(),
            "message": self._get_state_message(),
            "completed": self.completed_flag,
            "auto_reset": self.auto_reset_on_next_start,
            "target_finger": self._get_finger_name(self.current_finger)
        }

        # Добавляем countdown если в состоянии удержания
        if self.state == self.STATE_TOUCHING and self.current_touch_start_time:
            elapsed = time.time() - self.current_touch_start_time
            remaining = max(0, self.hold_duration - elapsed)
            data["countdown"] = int(remaining) + 1
            data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)
            logger.debug(f"Удержание: осталось {data['countdown']}с, прогресс {data['progress_percent']:.1f}%")

        return data

    def _calculate_distance(self, thumb_tip, finger_tip):
        """Вычисляет евклидово расстояние между кончиками пальцев"""
        dx = thumb_tip.x - finger_tip.x
        dy = thumb_tip.y - finger_tip.y
        return math.sqrt(dx*dx + dy*dy)

    def get_finger_states(self, hand_landmarks, frame_shape):
        """Получает позиции всех пальцев"""
        h, w, _ = frame_shape
        finger_tips = [4, 8, 12, 16, 20]

        tip_positions = []
        finger_states = [False] * 5

        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            x, y = int(tip.x * w), int(tip.y * h)
            tip_positions.append((x, y, tip))

        return finger_states, tip_positions

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """Проверяет касания пальцев"""
        # Проверяем, нужно ли автоматически сбросить упражнение
        self.check_and_reset_if_needed()

        current_time = time.time()

        # Получаем позиции всех пальцев
        finger_tips = [4, 8, 12, 16, 20]
        landmarks = [hand_landmarks.landmark[tip_idx] for tip_idx in finger_tips]
        thumb_tip = landmarks[0]

        # Если упражнение завершено
        if self.state == self.STATE_COMPLETED:
            self.completed_flag = True
            logger.debug("Упражнение завершено, ожидание команды")
            self.structured_data = self._get_structured_data()
            return True, self.structured_data["message"]

        # Определяем целевой палец
        target_finger_idx = self.current_finger + 1
        distance = self._calculate_distance(thumb_tip, landmarks[target_finger_idx])

        logger.debug(f"Расстояние до {self.FINGER_NAMES[target_finger_idx]}: {distance:.4f} (порог: {self.touch_threshold})")

        # Машина состояний
        if self.state == self.STATE_WAITING:
            if distance < self.touch_threshold:
                if current_time - self.last_touch_time > self.touch_cooldown:
                    self.state = self.STATE_TOUCHING
                    self.current_touch_start_time = current_time
                    self.last_touch_time = current_time
                    logger.info(f"Касание с {self.FINGER_NAMES[target_finger_idx]} засчитано, начинаем удержание")
            else:
                self.current_touch_start_time = None

        elif self.state == self.STATE_TOUCHING:
            if distance >= self.touch_threshold:
                self.state = self.STATE_WAITING
                self.current_touch_start_time = None
                logger.warning(f"Касание прервано, возврат к ожиданию")
            else:
                elapsed = current_time - self.current_touch_start_time

                if elapsed >= self.hold_duration:
                    self.touches_in_cycle += 1
                    logger.info(f"Касание с {self.FINGER_NAMES[target_finger_idx]} успешно завершено")

                    if self.touches_in_cycle >= 4:
                        self.current_cycle += 1
                        self.touches_in_cycle = 0
                        self.current_finger = 0
                        logger.info(f"Цикл {self.current_cycle}/{self.total_cycles} завершен")

                        if self.current_cycle >= self.total_cycles:
                            self.state = self.STATE_COMPLETED
                            self.completed_flag = True
                            self.auto_reset_on_next_start = True
                            logger.info("Упражнение полностью завершено!")
                        else:
                            self.state = self.STATE_WAITING
                            self.current_touch_start_time = None
                            logger.info(f"Начинаем цикл {self.current_cycle + 1}/{self.total_cycles}")
                    else:
                        self.current_finger += 1
                        self.state = self.STATE_WAITING
                        self.current_touch_start_time = None
                        logger.info(f"Следующий палец: {self.FINGER_NAMES[self.current_finger + 1]}")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()

        return True, self.structured_data["message"]

    def get_finger_colors(self, finger_states):
        """Возвращает цвета для каждого пальца в формате BGR"""
        colors = []

        for i in range(5):
            if i == 0:  # Большой палец
                colors.append((0, 255, 255))  # Желтый

            elif i == self.current_finger + 1:  # Целевой палец
                if self.state == self.STATE_TOUCHING:
                    colors.append((0, 255, 0))    # Зеленый - в процессе
                else:
                    colors.append((255, 255, 0))  # Голубой - ожидание

            else:
                colors.append((128, 128, 128))    # Серый - неактивный

        return colors

    def get_structured_data(self):
        """Возвращает структурированные данные для клиента"""
        return self.structured_data

    def force_reset_if_needed(self):
        """Принудительно сбрасывает упражнение если оно в состоянии completed"""
        if self.state == self.STATE_COMPLETED or self.completed_flag:
            logger.info(f"Принудительный сброс из состояния {self.state}")
            self.reset_for_new_attempt()
            return True
        return False