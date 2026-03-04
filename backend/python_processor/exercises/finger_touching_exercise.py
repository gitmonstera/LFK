"""
Упражнение "Считалочка" - поочередное касание пальцев с большим
"""

import time
import math
from .base_exercise import BaseExercise

class FingerTouchingExercise(BaseExercise):
    """Упражнение: Поочередное касание пальцев с большим"""

    def __init__(self):
        super().__init__()
        self.name = "Считалочка"
        self.description = "Поочередное касание пальцев - развивает мелкую моторику"
        self.exercise_id = "finger-touching"

        # Состояние упражнения
        self.state = "waiting"  # waiting, touching, completed
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
        self.touch_cooldown = 0.3  # задержка между касаниями (чтобы не засчитывать многократно)

        # Структурированные данные для клиента
        self.structured_data = self._get_structured_data()

        print(f"🔄 Упражнение инициализировано: {self.name}")

    def reset(self):
        """Сбрасывает упражнение в начальное состояние"""
        self.state = "waiting"
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
        print(f"🔄 Упражнение сброшено в начальное состояние")
        return True

    def reset_for_new_attempt(self):
        """Сбрасывает упражнение для нового подхода"""
        self.reset()
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

    def _get_finger_name(self, finger_index):
        """Возвращает название пальца по индексу"""
        names = ["указательным", "средним", "безымянным", "мизинцем"]
        return names[finger_index] if 0 <= finger_index < len(names) else "неизвестным"

    def _get_state_name(self):
        """Возвращает название текущего состояния"""
        if self.state == "waiting":
            return "Ожидание касания"
        elif self.state == "touching":
            return f"Касание с {self._get_finger_name(self.current_finger)}"
        elif self.state == "completed":
            return "Упражнение завершено"
        return "Неизвестно"

    def _get_cycle_progress(self):
        """Возвращает прогресс в текущем цикле"""
        return self.touches_in_cycle / 4.0

    def _get_state_message(self):
        """Возвращает сообщение для текущего состояния"""
        if self.state == "completed":
            return f"🎉 Упражнение завершено! Выполнено {self.total_cycles} циклов"

        # Общий прогресс
        total_touches_done = self.current_cycle * 4 + self.touches_in_cycle
        total_touches_needed = self.total_cycles * 4
        progress_percent = int((total_touches_done / total_touches_needed) * 100)

        # Текущий целевой палец
        next_finger = self.current_finger
        finger_name = self._get_finger_name(next_finger)

        if self.state == "waiting":
            if self.touches_in_cycle == 0 and self.current_cycle == 0:
                return f"👆 Коснитесь большим пальцем указательного (цикл {self.current_cycle + 1}/{self.total_cycles})"
            else:
                return f"👆 Коснитесь большим пальцем с {finger_name} (цикл {self.current_cycle + 1}/{self.total_cycles})"
        elif self.state == "touching":
            remaining = int(self.hold_duration - (time.time() - self.current_touch_start_time)) + 1
            return f"✅ Держите касание с {finger_name}... {remaining}с ({progress_percent}%)"

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
            "progress_percent": 0,
            "message": self._get_state_message(),
            "completed": self.completed_flag,
            "auto_reset": self.auto_reset_on_next_start,
            "target_finger": self._get_finger_name(self.current_finger)
        }

        # Добавляем countdown если в состоянии удержания
        if self.state == "touching" and self.current_touch_start_time:
            elapsed = time.time() - self.current_touch_start_time
            remaining = max(0, self.hold_duration - elapsed)
            data["countdown"] = int(remaining) + 1
            data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)
            data["message"] = self._get_state_message()

        return data

    def _calculate_distance(self, thumb_tip, finger_tip):
        """
        Вычисляет евклидово расстояние между кончиком большого пальца и целевым пальцем
        """
        dx = thumb_tip.x - finger_tip.x
        dy = thumb_tip.y - finger_tip.y
        return math.sqrt(dx*dx + dy*dy)

    def get_finger_states(self, hand_landmarks, frame_shape):
        """
        Переопределяем метод для получения состояний пальцев
        """
        h, w, _ = frame_shape
        finger_tips = [4, 8, 12, 16, 20]  # большой, указательный, средний, безымянный, мизинец

        tip_positions = []
        finger_states = [False] * 5  # здесь мы не используем стандартные состояния

        # Получаем позиции всех кончиков пальцев
        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            x, y = int(tip.x * w), int(tip.y * h)
            tip_positions.append((x, y, tip))  # сохраняем и координаты, и объект landmark

        return finger_states, tip_positions

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """
        Проверяет касания пальцев
        """
        # Проверяем, нужно ли автоматически сбросить упражнение
        self.check_and_reset_if_needed()

        current_time = time.time()

        # Получаем позиции всех пальцев
        finger_tips = [4, 8, 12, 16, 20]  # большой, указательный, средний, безымянный, мизинец
        landmarks = []

        for tip_idx in finger_tips:
            landmarks.append(hand_landmarks.landmark[tip_idx])

        thumb_tip = landmarks[0]  # большой палец

        # Если упражнение завершено, просто возвращаем сообщение
        if self.state == "completed":
            self.completed_flag = True
            message = f"🎉 Упражнение завершено! Выполнено {self.total_cycles} циклов"
            print(f"   🔍 Упражнение завершено")
            self.structured_data = self._get_structured_data()
            return True, message

        # Определяем целевой палец (индекс 1-4)
        target_finger_idx = self.current_finger + 1  # +1 потому что 0 - большой палец

        # Вычисляем расстояние до целевого пальца
        distance = self._calculate_distance(thumb_tip, landmarks[target_finger_idx])

        # Отладочный вывод
        finger_names = ["большой", "указательный", "средний", "безымянный", "мизинец"]
        print(f"   📏 Расстояние до {finger_names[target_finger_idx]}: {distance:.4f}")

        # Машина состояний
        if self.state == "waiting":
            # Ждем пока пользователь коснется целевого пальца
            if distance < self.touch_threshold:
                # Проверяем cooldown чтобы не засчитывать многократно
                if current_time - self.last_touch_time > self.touch_cooldown:
                    self.state = "touching"
                    self.current_touch_start_time = current_time
                    self.last_touch_time = current_time
                    print(f"   ✅ Касание засчитано! Начинаем отсчет...")
            else:
                # Сбрасываем таймер если палец отошел
                self.current_touch_start_time = None

        elif self.state == "touching":
            if distance >= self.touch_threshold:
                # Если палец отошел раньше времени
                self.state = "waiting"
                self.current_touch_start_time = None
                print(f"   ❌ Касание прервано! Возврат к ожиданию")
            else:
                # Держим касание с обратным отсчетом
                elapsed = current_time - self.current_touch_start_time

                # Если время вышло - касание успешно завершено
                if elapsed >= self.hold_duration:
                    # Переходим к следующему пальцу
                    self.touches_in_cycle += 1

                    if self.touches_in_cycle >= 4:
                        # Завершен полный цикл (все 4 пальца)
                        self.current_cycle += 1
                        self.touches_in_cycle = 0
                        self.current_finger = 0
                        print(f"   ✅ ЦИКЛ {self.current_cycle}/{self.total_cycles} ЗАВЕРШЕН!")

                        if self.current_cycle >= self.total_cycles:
                            # Все циклы выполнены
                            self.state = "completed"
                            self.completed_flag = True
                            self.auto_reset_on_next_start = True
                            print(f"   🎉 УПРАЖНЕНИЕ ПОЛНОСТЬЮ ЗАВЕРШЕНО!")
                        else:
                            # Переходим к следующему циклу
                            self.state = "waiting"
                            self.current_touch_start_time = None
                            print(f"   🔄 Начинаем цикл {self.current_cycle + 1}")
                    else:
                        # Переходим к следующему пальцу в этом же цикле
                        self.current_finger += 1
                        self.state = "waiting"
                        self.current_touch_start_time = None
                        print(f"   👉 Следующий палец: {finger_names[self.current_finger + 1]}")

        # Обновляем структурированные данные
        self.structured_data = self._get_structured_data()
        message = self.structured_data["message"]

        return True, message

    def get_finger_colors(self, finger_states):
        """
        Возвращает цвета для каждого пальца
        """
        colors = []
        finger_names = ["большой", "указательный", "средний", "безымянный", "мизинец"]

        for i in range(5):
            if i == 0:  # Большой палец
                colors.append((0, 255, 255))  # Желтый - активный
            elif i == self.current_finger + 1:  # Целевой палец
                if self.state == "touching":
                    colors.append((0, 255, 0))  # Зеленый - в процессе касания
                else:
                    colors.append((255, 255, 0))  # Голубой - ожидание касания
            else:
                colors.append((128, 128, 128))  # Серый - неактивный

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