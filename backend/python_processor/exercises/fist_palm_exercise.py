import cv2
import numpy as np
import time
from .base_exercise import BaseExercise

class FistPalmExercise(BaseExercise):
    """Упражнение: Кулак-ладонь (для кровообращения)"""

    def __init__(self):
        super().__init__()
        self.name = "Кулак-ладонь"
        self.description = "Сжимайте и разжимайте пальцы для улучшения кровообращения"
        self.exercise_id = "fist-palm"

        # Состояние упражнения
        self.state = "waiting_fist"  # waiting_fist, holding_fist, waiting_palm, holding_palm, completed
        self.state_start_time = time.time()
        self.hold_duration = 3  # секунд удержания

        # Счетчики
        self.current_cycle = 0
        self.total_cycles = 5

        # Таймер для обратного отсчета
        self.countdown = 3
        self.last_countdown_update = time.time()

        # Флаг для отслеживания завершения цикла
        self.cycle_completed = False
        self.completed_flag = False

        # Флаг для автоматического сброса при новом подключении
        self.auto_reset_on_next_start = False

        # Структурированные данные для клиента
        self.structured_data = self._get_structured_data()

        print(f"🔄 Упражнение инициализировано: {self.name}")

    def reset(self):
        """Сбрасывает упражнение в начальное состояние"""
        self.state = "waiting_fist"
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.structured_data = self._get_structured_data()
        print(f"🔄 Упражнение сброшено в начальное состояние")
        return True

    def reset_for_new_attempt(self):
        """Сбрасывает упражнение для нового подхода (без полной перезагрузки)"""
        self.state = "waiting_fist"
        self.state_start_time = time.time()
        self.current_cycle = 0
        self.countdown = self.hold_duration
        self.last_countdown_update = time.time()
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.structured_data = self._get_structured_data()
        print(f"🔄 Упражнение сброшено для нового подхода")
        return True

    def mark_for_reset(self):
        """Помечает упражнение для сброса при следующем запуске"""
        self.auto_reset_on_next_start = True
        print(f"🔄 Упражнение помечено для сброса при следующем запуске")

    def check_and_reset_if_needed(self):
        """Проверяет, нужно ли сбросить упражнение, и сбрасывает если да"""
        if self.auto_reset_on_next_start:
            print(f"🔄 Автоматический сброс упражнения при новом запуске")
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
        if self.state in ["holding_fist", "holding_palm"]:
            data["countdown"] = self.countdown
            # Прогресс в процентах
            elapsed = time.time() - self.state_start_time
            data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)
            print(f"   ⏱️ structured_data: countdown={self.countdown}, progress={data['progress_percent']:.1f}%")

        return data

    def _get_state_name(self):
        """Возвращает название текущего состояния"""
        state_names = {
            "waiting_fist": "Ожидание кулака",
            "holding_fist": "Держите кулак",
            "waiting_palm": "Ожидание ладони",
            "holding_palm": "Держите ладонь",
            "completed": "Упражнение завершено"
        }
        return state_names.get(self.state, "Неизвестно")

    def _get_state_message(self):
        """Возвращает сообщение для текущего состояния"""
        # Показываем следующий цикл (current_cycle + 1)
        next_cycle = self.current_cycle + 1
        if next_cycle > self.total_cycles:
            next_cycle = self.total_cycles

        if self.state == "waiting_fist":
            return f"👊 ШАГ 1/4: Сожмите кулак (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == "holding_fist":
            return f"👊 ШАГ 2/4: Держите кулак... {self.countdown} (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == "waiting_palm":
            return f"🖐️ ШАГ 3/4: Раскройте ладонь (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == "holding_palm":
            return f"🖐️ ШАГ 4/4: Держите ладонь... {self.countdown} (цикл {next_cycle}/{self.total_cycles})"
        elif self.state == "completed":
            return f"🎉 Упражнение завершено! Выполнено {self.total_cycles} циклов."
        return ""

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """
        Проверяет положение пальцев и управляет состояниями упражнения
        """
        # Проверяем, нужно ли автоматически сбросить упражнение
        self.check_and_reset_if_needed()

        raised_fingers = sum(finger_states)
        current_time = time.time()

        # Определяем, кулак ли это (0-2 пальца поднято)
        is_fist = raised_fingers <= 0

        # Определяем, ладонь ли это (3-5 пальцев поднято)
        is_palm = raised_fingers >= 5

        # Сброс флага завершения цикла
        self.cycle_completed = False

        # Отладочный вывод
        print(f"   🔍 Текущее состояние: {self.state}")
        print(f"   🔍 Поднято пальцев: {raised_fingers}, is_fist={is_fist}, is_palm={is_palm}")
        print(f"   🔍 Текущий цикл: {self.current_cycle}/{self.total_cycles}")

        # Машина состояний
        if self.state == "waiting_fist":
            # Ждем пока пользователь сожмет кулак
            if is_fist:
                self.state = "holding_fist"
                self.state_start_time = current_time
                self.countdown = self.hold_duration
                print(f"   ✅ КУЛАК СЖАТ! Начинаем отсчет...")

        elif self.state == "holding_fist":
            # Держим кулак с обратным отсчетом
            if not is_fist:
                # Если разжал раньше времени - возвращаемся
                self.state = "waiting_fist"
                print(f"   ❌ Кулак разжат! Возврат к ожиданию")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed

                # Обновляем countdown (целые секунды)
                new_countdown = int(remaining) + 1
                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    print(f"   ⏱️  Осталось: {self.countdown}с")

                # Если время вышло - переходим к следующему шагу
                if elapsed >= self.hold_duration:
                    self.state = "waiting_palm"
                    self.state_start_time = current_time
                    self.countdown = self.hold_duration
                    print(f"   ✅ Фаза кулака завершена! Теперь разожмите ладонь")

        elif self.state == "waiting_palm":
            # Ждем пока пользователь разожмет ладонь
            if is_palm:
                self.state = "holding_palm"
                self.state_start_time = current_time
                self.countdown = self.hold_duration
                print(f"   ✅ ЛАДОНЬ РАСКРЫТА! Начинаем отсчет...")

        elif self.state == "holding_palm":
            # Держим ладонь с обратным отсчетом
            if not is_palm:
                # Если сжал раньше времени - возвращаемся
                self.state = "waiting_palm"
                print(f"   ❌ Ладонь сжата! Возврат к ожиданию")
            else:
                elapsed = current_time - self.state_start_time
                remaining = self.hold_duration - elapsed

                # Обновляем countdown (целые секунды)
                new_countdown = int(remaining) + 1
                if new_countdown != self.countdown:
                    self.countdown = new_countdown
                    print(f"   ⏱️  Осталось: {self.countdown}с")

                # Если время вышло - завершаем цикл
                if elapsed >= self.hold_duration:
                    self.current_cycle += 1
                    self.cycle_completed = True
                    print(f"   ✅ ЦИКЛ {self.current_cycle}/{self.total_cycles} ЗАВЕРШЕН!")

                    if self.current_cycle >= self.total_cycles:
                        # Все циклы выполнены - завершаем упражнение
                        self.state = "completed"
                        self.completed_flag = True
                        print(f"   🎉 УПРАЖНЕНИЕ ПОЛНОСТЬЮ ЗАВЕРШЕНО!")

                        # АВТОМАТИЧЕСКИ СБРАСЫВАЕМ ДЛЯ СЛЕДУЮЩЕГО ЗАПУСКА
                        self.auto_reset_on_next_start = True
                        print(f"   🔄 Упражнение помечено для сброса при следующем запуске")
                    else:
                        # Переходим к следующему циклу
                        self.state = "waiting_fist"
                        self.state_start_time = current_time
                        self.countdown = self.hold_duration
                        print(f"   🔄 Начинаем цикл {self.current_cycle + 1}/{self.total_cycles}")

        elif self.state == "completed":
            # Если упражнение завершено, просто возвращаем сообщение о завершении
            print(f"   🔍 Упражнение завершено, ожидание команды от клиента")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()

        # Формируем сообщение
        message = self.structured_data["message"]

        return True, message

    def get_finger_colors(self, finger_states):
        """
        Возвращает цвета для каждого пальца
        Должен возвращать список из 5 цветов в формате BGR
        """
        colors = []

        for i, is_raised in enumerate(finger_states):
            if self.state in ["waiting_fist", "holding_fist"]:
                # В фазе кулака: пальцы должны быть сжаты
                if is_raised:
                    colors.append((0, 0, 255))  # Красный - ошибка
                else:
                    colors.append((0, 255, 0))  # Зеленый - правильно
            elif self.state in ["waiting_palm", "holding_palm"]:
                # В фазе ладони: пальцы должны быть подняты
                if is_raised:
                    colors.append((0, 255, 0))  # Зеленый - правильно
                else:
                    colors.append((0, 0, 255))  # Красный - ошибка
            else:  # completed
                # В завершенном состоянии все пальцы серые
                colors.append((128, 128, 128))

        return colors

    def get_structured_data(self):
        """Возвращает структурированные данные для клиента"""
        return self.structured_data

    def force_reset_if_needed(self):
        """Принудительно сбрасывает упражнение если оно в состоянии completed"""
        if self.state == "completed" or self.completed_flag:
            print(f"🔄 Принудительный сброс упражнения из состояния {self.state}")
            self.reset_for_new_attempt()
            return True
        return False