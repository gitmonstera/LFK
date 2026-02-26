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
        self.structured_data = self._get_structured_data()
        print(f"🔄 Упражнение сброшено в начальное состояние")

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
            "completed": self.completed_flag
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
            return f"🎉 Упражнение завершено! Выполнено {self.total_cycles} циклов. Нажмите R для повторения"
        return ""

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """
        Проверяет положение пальцев и управляет состояниями упражнения
        """
        raised_fingers = sum(finger_states)
        current_time = time.time()

        # Определяем, кулак ли это (0-2 пальца поднято)
        is_fist = raised_fingers <= 2

        # Определяем, ладонь ли это (3-5 пальцев поднято)
        is_palm = raised_fingers >= 3

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
                        # Все циклы выполнены - переходим в состояние completed
                        self.state = "completed"
                        self.completed_flag = True
                        print(f"   🎉 УПРАЖНЕНИЕ ПОЛНОСТЬЮ ЗАВЕРШЕНО!")
                        # НЕ СБРАСЫВАЕМ АВТОМАТИЧЕСКИ!
                    else:
                        # Переходим к следующему циклу
                        self.state = "waiting_fist"
                        self.state_start_time = current_time
                        self.countdown = self.hold_duration
                        print(f"   🔄 Начинаем цикл {self.current_cycle + 1}/{self.total_cycles}")

        elif self.state == "completed":
            # Если упражнение завершено, просто возвращаем сообщение о завершении
            # НЕ МЕНЯЕМ СОСТОЯНИЕ!
            print(f"   🔍 Упражнение завершено, ожидание команды от клиента")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()

        # Формируем сообщение
        message = self.structured_data["message"]

        return True, message

    def get_finger_colors(self, finger_states):
        """
        Цвета для упражнения Кулак-ладонь
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

    def draw_feedback(self, frame, finger_states, tip_positions, is_correct, message):
        """Рисует обратную связь на кадре"""
        h, w, _ = frame.shape

        # Получаем цвета для пальцев
        finger_colors = self.get_finger_colors(finger_states)

        # Рисуем точки на кончиках пальцев
        for i, (x, y) in enumerate(tip_positions):
            color = finger_colors[i]

            cv2.circle(frame, (x, y), 20, color, -1)
            cv2.circle(frame, (x, y), 20, (255, 255, 255), 2)

            status = "⬆️" if finger_states[i] else "⬇️"
            cv2.putText(frame, f"{i}{status}", (x-20, y-25),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        # Информация на кадре
        cv2.rectangle(frame, (5, 5), (650, 280), (0, 0, 0), -1)
        cv2.rectangle(frame, (5, 5), (650, 280), (255, 255, 255), 2)

        info_y = 30
        cv2.putText(frame, f"Упражнение: {self.name}", (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        info_y += 30
        # Отображаем текущее состояние
        if self.state == "completed":
            state_color = (0, 255, 0)  # Зеленый для завершения
        else:
            state_color = (0, 255, 255) if "holding" in self.state else (255, 255, 255)
        cv2.putText(frame, self._get_state_name(), (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.8, state_color, 2)

        info_y += 30
        if self.state != "completed":
            # Показываем следующий цикл (current_cycle + 1)
            next_cycle = self.current_cycle + 1
            if next_cycle > self.total_cycles:
                next_cycle = self.total_cycles
            cv2.putText(frame, f"Цикл: {next_cycle}/{self.total_cycles}", (15, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)
        else:
            cv2.putText(frame, f"Выполнено: {self.total_cycles} циклов", (15, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

        # Прогресс-бар для удержания
        if self.state in ["holding_fist", "holding_palm"] and self.structured_data["countdown"]:
            info_y += 40
            bar_x = 15
            bar_y = info_y
            bar_width = 500
            bar_height = 40

            # Рамка
            cv2.rectangle(frame, (bar_x, bar_y), (bar_x + bar_width, bar_y + bar_height), (255, 255, 255), 2)

            # Заполнение
            fill_width = int((self.structured_data["progress_percent"] / 100) * bar_width)
            if fill_width > 0:
                color = (0, 255, 255) if "fist" in self.state else (255, 255, 0)
                cv2.rectangle(frame, (bar_x, bar_y), (bar_x + fill_width, bar_y + bar_height), color, -1)

            # Большой текст счетчика
            countdown_text = str(self.structured_data["countdown"])
            text_size = cv2.getTextSize(countdown_text, cv2.FONT_HERSHEY_SIMPLEX, 2, 3)[0]
            text_x = bar_x + (bar_width - text_size[0]) // 2
            text_y = bar_y + bar_height - 10
            cv2.putText(frame, countdown_text, (text_x, text_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 2, (255, 255, 255), 3)

        # Сообщение
        info_y = h - 60
        if "❌" in message:
            cv2.putText(frame, message, (15, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 0, 255), 2)
        elif "✅" in message or "🎉" in message:
            cv2.putText(frame, message, (15, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
        else:
            cv2.putText(frame, message, (15, info_y),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        return frame

    def get_structured_data(self):
        """Возвращает структурированные данные для клиента"""
        return self.structured_data