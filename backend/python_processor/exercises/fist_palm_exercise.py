"""
Упражнение: Кулак-ладонь
Чередование сжатия и разжатия пальцев для улучшения кровообращения
"""

import time
import logging
from .base_exercise import BaseExercise

# Настройка логгера для этого упражнения
logger = logging.getLogger('LKF.Exercise.FistPalm')

class FistPalmExercise(BaseExercise):
    """Упражнение: Кулак-ладонь (для кровообращения)"""

    # Константы для состояний
    STATE_WAITING_FIST = "waiting_fist"
    STATE_HOLDING_FIST = "holding_fist"
    STATE_WAITING_PALM = "waiting_palm"
    STATE_HOLDING_PALM = "holding_palm"
    STATE_COMPLETED = "completed"

    # Названия состояний для отображения
    STATE_NAMES = {
        STATE_WAITING_FIST: "Ожидание кулака",
        STATE_HOLDING_FIST: "Держите кулак",
        STATE_WAITING_PALM: "Ожидание ладони",
        STATE_HOLDING_PALM: "Держите ладонь",
        STATE_COMPLETED: "Упражнение завершено"
    }

    def __init__(self):
        super().__init__()
        self.name = "Кулак-ладонь"
        self.description = "Сжимайте и разжимайте пальцы для улучшения кровообращения"
        self.exercise_id = "fist-palm"

        # Состояние упражнения
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = time.time()
        self.hold_duration = 3  # секунд удержания

        # Счетчики
        self.current_cycle = 0
        self.total_cycles = 5

        # Таймер для обратного отсчета
        self.countdown = 3
        self.last_countdown_update = time.time()

        # Флаги состояния
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False

        # Структурированные данные для клиента
        self.structured_data = self._get_structured_data()

        logger.info(f"Упражнение инициализировано: {self.name}")
        logger.debug(f"Параметры: циклов={self.total_cycles}, удержание={self.hold_duration}с")

    def reset(self):
        """Сбрасывает упражнение в начальное состояние"""
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.structured_data = self._get_structured_data()

        logger.info("Упражнение сброшено в начальное состояние")
        return True

    def reset_for_new_attempt(self):
        """Сбрасывает упражнение для нового подхода"""
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.structured_data = self._get_structured_data()

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

    def _get_structured_data(self):
        """Формирует структурированные данные для клиента"""
        data = {
            "state": self.state,
            "state_name": self._get_state_name(),
            "current_cycle": self.current_cycle,
            "total_cycles": self.total_cycles,
            "countdown": None,
            "progress_percent": 0,
            "message": self._get_state_message(),
            "cycle_completed": self.cycle_completed,
            "completed": self.completed_flag,
            "auto_reset": self.auto_reset_on_next_start
        }

        # Добавляем countdown если в состоянии удержания
        if self.state in [self.STATE_HOLDING_FIST, self.STATE_HOLDING_PALM]:
            data["countdown"] = self.countdown
            elapsed = time.time() - self.state_start_time
            data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)
            logger.debug(f"Прогресс: {data['progress_percent']:.1f}%, осталось: {self.countdown}с")

        return data

    def _get_state_name(self):
        """Возвращает название текущего состояния"""
        return self.STATE_NAMES.get(self.state, "Неизвестно")

    def _get_state_message(self):
        """Возвращает сообщение для текущего состояния"""
        next_cycle = min(self.current_cycle + 1, self.total_cycles)

        messages = {
            self.STATE_WAITING_FIST: f"Сожмите кулак (цикл {next_cycle}/{self.total_cycles})",
            self.STATE_HOLDING_FIST: f"Держите кулак... {self.countdown}с (цикл {next_cycle}/{self.total_cycles})",
            self.STATE_WAITING_PALM: f"Раскройте ладонь (цикл {next_cycle}/{self.total_cycles})",
            self.STATE_HOLDING_PALM: f"Держите ладонь... {self.countdown}с (цикл {next_cycle}/{self.total_cycles})",
            self.STATE_COMPLETED: f"Упражнение завершено! Выполнено {self.total_cycles} циклов"
        }

        return messages.get(self.state, "")

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """
        Проверяет положение пальцев и управляет состояниями упражнения
        """
        # Проверяем, нужно ли автоматически сбросить упражнение
        self.check_and_reset_if_needed()

        raised_fingers = sum(finger_states)
        current_time = time.time()

        # Определяем тип положения
        is_fist = raised_fingers <= 0
        is_palm = raised_fingers >= 5

        # Сброс флага завершения цикла
        self.cycle_completed = False

        # Отладочная информация
        logger.debug(f"Состояние: {self.state}, пальцев: {raised_fingers}, цикл: {self.current_cycle}/{self.total_cycles}")

        # Машина состояний
        if self.state == self.STATE_WAITING_FIST:
            if is_fist:
                self.state = self.STATE_HOLDING_FIST
                self.state_start_time = current_time
                self.countdown = self.hold_duration
                logger.info("Кулак сжат, начинаем удержание")

        elif self.state == self.STATE_HOLDING_FIST:
            if not is_fist:
                self.state = self.STATE_WAITING_FIST
                logger.warning("Кулак разжат раньше времени, возврат к ожиданию")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed
                new_countdown = int(remaining) + 1

                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    logger.debug(f"Удержание кулака: осталось {self.countdown}с")

                if elapsed >= self.hold_duration:
                    self.state = self.STATE_WAITING_PALM
                    self.state_start_time = current_time
                    self.countdown = self.hold_duration
                    logger.info("Фаза кулака завершена, ожидание ладони")

        elif self.state == self.STATE_WAITING_PALM:
            if is_palm:
                self.state = self.STATE_HOLDING_PALM
                self.state_start_time = current_time
                self.countdown = self.hold_duration
                logger.info("Ладонь раскрыта, начинаем удержание")

        elif self.state == self.STATE_HOLDING_PALM:
            if not is_palm:
                self.state = self.STATE_WAITING_PALM
                logger.warning("Ладонь сжата раньше времени, возврат к ожиданию")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed
                new_countdown = int(remaining) + 1

                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    logger.debug(f"Удержание ладони: осталось {self.countdown}с")

                if elapsed >= self.hold_duration:
                    self.current_cycle += 1
                    self.cycle_completed = True
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
                        logger.info(f"Начинаем цикл {self.current_cycle + 1}/{self.total_cycles}")

        elif self.state == self.STATE_COMPLETED:
            logger.debug("Упражнение завершено, ожидание команды")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()
        message = self.structured_data["message"]

        return True, message

    def get_finger_colors(self, finger_states):
        """
        Возвращает цвета для каждого пальца в формате BGR
        """
        colors = []

        for i, is_raised in enumerate(finger_states):
            if self.state in [self.STATE_WAITING_FIST, self.STATE_HOLDING_FIST]:
                # В фазе кулака: пальцы должны быть сжаты
                if is_raised:
                    colors.append((0, 0, 255))      # Красный - ошибка
                else:
                    colors.append((0, 255, 0))      # Зеленый - правильно

            elif self.state in [self.STATE_WAITING_PALM, self.STATE_HOLDING_PALM]:
                # В фазе ладони: пальцы должны быть подняты
                if is_raised:
                    colors.append((0, 255, 0))      # Зеленый - правильно
                else:
                    colors.append((0, 0, 255))      # Красный - ошибка

            else:  # completed
                colors.append((128, 128, 128))       # Серый - нейтральный

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