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

    # Константы для состояний - как в fist-palm
    STATE_WAITING_FIST = "waiting_fist"
    STATE_HOLDING_FIST = "holding_fist"
    STATE_WAITING_PALM = "waiting_palm"
    STATE_HOLDING_PALM = "holding_palm"
    STATE_COMPLETED = "completed"

    # Названия состояний для отображения
    STATE_NAMES = {
        STATE_WAITING_FIST: "Ожидание касания указательного",
        STATE_HOLDING_FIST: "Удержание указательного",
        STATE_WAITING_PALM: "Ожидание следующего пальца",
        STATE_HOLDING_PALM: "Удержание пальца",
        STATE_COMPLETED: "Упражнение завершено"
    }

    def __init__(self):
        super().__init__()
        self.name = "Считалочка"
        self.description = "Поочередное касание пальцев - развивает мелкую моторику"
        self.exercise_id = "finger-touching"

        # Состояние упражнения - как в fist-palm
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = time.time()
        self.hold_duration = 1.0  # секунд удержания

        # Счетчики
        self.current_cycle = 0
        self.total_cycles = 5

        # Таймер для обратного отсчета
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()

        # Флаги состояния
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False

        # Для отслеживания текущего пальца
        self.current_finger = 0  # 0=указательный, 1=средний, 2=безымянный, 3=мизинец

        # Пороговое расстояние для касания (в относительных координатах)
        self.touch_threshold = 0.05

        # Для отслеживания касания
        self.current_touch_start_time = None
        self.last_touch_time = 0
        self.touch_cooldown = 0.3

        # Структурированные данные для клиента
        self.structured_data = self._get_structured_data()

        logger.info(f"Упражнение инициализировано: {self.name}")
        logger.debug(f"Параметры: циклов={self.total_cycles}, удержание={self.hold_duration}с")

    def reset(self):
        """Сбрасывает упражнение в начальное состояние"""
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.current_finger = 0
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.current_touch_start_time = None
        self.last_touch_time = 0
        self.structured_data = self._get_structured_data()

        logger.info("Упражнение сброшено в начальное состояние")
        return True

    def get_finger_states(self, hand_landmarks, frame_shape):
        """
        Получает позиции всех пальцев - переопределяем метод из BaseExercise
        чтобы возвращать только координаты (x, y), а не (x, y, tip)
        """
        h, w, _ = frame_shape
        finger_tips = [4, 8, 12, 16, 20]

        tip_positions = []
        finger_states = [False] * 5

        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            x, y = int(tip.x * w), int(tip.y * h)
            tip_positions.append((x, y))  # Только координаты, без tip

            # Определяем поднят ли палец (как в базовом классе)
            if i == 0:  # Большой палец
                index_mcp = hand_landmarks.landmark[5]
                dist = abs(tip.x - index_mcp.x) + abs(tip.y - index_mcp.y)
                finger_states[i] = dist > 0.15
            else:
                pip = hand_landmarks.landmark[finger_tips[i] - 1]
                finger_states[i] = tip.y < pip.y - 0.02

        return finger_states, tip_positions

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

    def _get_state_name(self):
        """Возвращает название текущего состояния"""
        return self.STATE_NAMES.get(self.state, "Неизвестно")

    def _get_progress_percent(self):
        """Возвращает общий прогресс в процентах"""
        total_touches = self.current_cycle * 4 + self.current_finger
        total_needed = self.total_cycles * 4
        return int((total_touches / total_needed) * 100) if total_needed > 0 else 0

    def _get_state_message(self):
        """Возвращает сообщение для текущего состояния"""
        next_cycle = min(self.current_cycle + 1, self.total_cycles)

        finger_names = ["указательным", "средним", "безымянным", "мизинцем"]

        if self.state == self.STATE_WAITING_FIST:
            return f"Коснитесь большим пальцем указательного (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_HOLDING_FIST:
            return f"Держите касание с указательным... {self.countdown}с (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_WAITING_PALM:
            finger_name = finger_names[self.current_finger]
            return f"Коснитесь большим пальцем с {finger_name} (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_HOLDING_PALM:
            finger_name = finger_names[self.current_finger]
            return f"Держите касание с {finger_name}... {self.countdown}с (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_COMPLETED:
            return f"Упражнение завершено! Выполнено {self.total_cycles} циклов"
        return ""

    def _get_structured_data(self):
        """Формирует структурированные данные для клиента - идентично fist-palm"""
        data = {
            "state": self.state,
            "state_name": self._get_state_name(),
            "current_cycle": self.current_cycle,
            "total_cycles": self.total_cycles,
            "countdown": None,
            "progress_percent": self._get_progress_percent(),
            "message": self._get_state_message(),
            "cycle_completed": self.cycle_completed,
            "completed": self.completed_flag,
            "auto_reset": self.auto_reset_on_next_start
        }

        # Добавляем countdown если в состоянии удержания
        if self.state in [self.STATE_HOLDING_FIST, self.STATE_HOLDING_PALM]:
            data["countdown"] = self.countdown
            if self.current_touch_start_time:
                elapsed = time.time() - self.current_touch_start_time
                data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)
            logger.debug(f"Удержание: осталось {self.countdown}с, прогресс {data['progress_percent']:.1f}%")

        return data

    def _calculate_distance(self, thumb_tip, finger_tip):
        """Вычисляет евклидово расстояние между кончиками пальцев"""
        dx = thumb_tip.x - finger_tip.x
        dy = thumb_tip.y - finger_tip.y
        return math.sqrt(dx*dx + dy*dy)

    def get_finger_colors(self, finger_states):
        """Возвращает цвета для каждого пальца в формате BGR"""
        colors = []

        for i, is_raised in enumerate(finger_states):
            if i == 0:  # Большой палец - всегда желтый
                colors.append((0, 255, 255))
                continue

            target_finger = self.current_finger + 1

            if self.state in [self.STATE_WAITING_FIST, self.STATE_HOLDING_FIST]:
                # В фазе ожидания/удержания указательного
                if i == 1:  # указательный
                    if self.state == self.STATE_HOLDING_FIST:
                        colors.append((0, 255, 0))  # Зеленый - в процессе
                    else:
                        colors.append((255, 255, 0))  # Голубой - ожидание
                else:
                    colors.append((128, 128, 128))  # Серый

            elif self.state in [self.STATE_WAITING_PALM, self.STATE_HOLDING_PALM]:
                # В фазе ожидания/удержания других пальцев
                if i < target_finger:
                    colors.append((0, 255, 0))  # Зеленый - уже выполнено
                elif i == target_finger:
                    if self.state == self.STATE_HOLDING_PALM:
                        colors.append((0, 255, 0))  # Зеленый - в процессе
                    else:
                        colors.append((255, 255, 0))  # Голубой - ожидание
                else:
                    colors.append((128, 128, 128))  # Серый

            else:  # completed
                colors.append((128, 128, 128))

        return colors

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """Проверяет касания пальцев - машина состояний как в fist-palm"""
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

        # Сброс флага завершения цикла
        self.cycle_completed = False

        # Машина состояний
        if self.state == self.STATE_WAITING_FIST:
            if distance < self.touch_threshold:
                if current_time - self.last_touch_time > self.touch_cooldown:
                    self.state = self.STATE_HOLDING_FIST
                    self.state_start_time = current_time
                    self.current_touch_start_time = current_time
                    self.last_touch_time = current_time
                    self.countdown = self.hold_duration
                    logger.info("Касание с указательным засчитано, начинаем удержание")

        elif self.state == self.STATE_HOLDING_FIST:
            if distance >= self.touch_threshold:
                self.state = self.STATE_WAITING_FIST
                self.current_touch_start_time = None
                logger.warning("Касание прервано, возврат к ожиданию")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed
                new_countdown = int(remaining) + 1

                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    logger.debug(f"Удержание указательного: осталось {self.countdown}с")

                if elapsed >= self.hold_duration:
                    self.current_finger += 1
                    self.state = self.STATE_WAITING_PALM
                    self.state_start_time = current_time
                    self.countdown = self.hold_duration
                    self.current_touch_start_time = None
                    logger.info(f"Указательный завершен, следующий палец: {self.current_finger}")

        elif self.state == self.STATE_WAITING_PALM:
            if distance < self.touch_threshold:
                if current_time - self.last_touch_time > self.touch_cooldown:
                    self.state = self.STATE_HOLDING_PALM
                    self.state_start_time = current_time
                    self.current_touch_start_time = current_time
                    self.last_touch_time = current_time
                    self.countdown = self.hold_duration
                    logger.info(f"Касание с пальцем {self.current_finger} засчитано, начинаем удержание")

        elif self.state == self.STATE_HOLDING_PALM:
            if distance >= self.touch_threshold:
                self.state = self.STATE_WAITING_PALM
                self.current_touch_start_time = None
                logger.warning(f"Касание с пальцем {self.current_finger} прервано")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed
                new_countdown = int(remaining) + 1

                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    logger.debug(f"Удержание пальца {self.current_finger}: осталось {self.countdown}с")

                if elapsed >= self.hold_duration:
                    self.current_finger += 1
                    self.cycle_completed = True
                    logger.info(f"Палец {self.current_finger-1} завершен")

                    if self.current_finger >= 4:
                        self.current_cycle += 1
                        self.current_finger = 0
                        logger.info(f"Цикл {self.current_cycle}/{self.total_cycles} завершен")

                        if self.current_cycle >= self.total_cycles:
                            self.state = self.STATE_COMPLETED
                            self.completed_flag = True
                            self.auto_reset_on_next_start = True
                            logger.info("Упражнение полностью завершено!")
                        else:
                            self.state = self.STATE_WAITING_FIST
                            self.state_start_time = current_time
                            self.countdown = self.hold_duration
                            self.current_touch_start_time = None
                            logger.info(f"Начинаем цикл {self.current_cycle + 1}/{self.total_cycles}")
                    else:
                        self.state = self.STATE_WAITING_PALM
                        self.state_start_time = current_time
                        self.countdown = self.hold_duration
                        self.current_touch_start_time = None
                        logger.info(f"Переход к следующему пальцу: {self.current_finger}")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()
        message = self.structured_data["message"]

        return True, message

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