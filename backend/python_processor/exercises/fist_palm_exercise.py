"""
Упражнение: Кулак-ладонь (МИНИМАЛЬНАЯ ОТРИСОВКА)
Чередование сжатия и разжатия пальцев для улучшения кровообращения
"""

import time
import logging

import cv2

from .base_exercise import BaseExercise

logger = logging.getLogger('LFK.Exercise.FistPalm')

class FistPalmExercise(BaseExercise):
    """Упражнение: Кулак-ладонь - минимальная отрисовка"""

    # Константы для состояний
    STATE_WAITING_FIST = "waiting_fist"
    STATE_HOLDING_FIST = "holding_fist"
    STATE_WAITING_PALM = "waiting_palm"
    STATE_HOLDING_PALM = "holding_palm"
    STATE_COMPLETED = "completed"

    # Предопределенные цвета
    COLORS = {
        'green': (0, 255, 0),
        'red': (0, 0, 255),
        'gray': (128, 128, 128),
        'white': (255, 255, 255),
        'black': (0, 0, 0)
    }

    def __init__(self):
        super().__init__()
        self.name = "Кулак-ладонь"
        self.description = "Сжимайте и разжимайте пальцы"
        self.exercise_id = "fist-palm"

        # Состояние
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = 0.0
        self.hold_duration = 2.5

        # Счетчики
        self.current_cycle = 0
        self.total_cycles = 5

        # Таймер
        self.countdown = self.hold_duration

        # Флаги
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False

        self.structured_data = self._get_structured_data()
        logger.info(f"Упражнение инициализировано: {self.name}")

    def reset(self):
        self.state = self.STATE_WAITING_FIST
        self.state_start_time = 0.0
        self.current_cycle = 0
        self.countdown = self.hold_duration
        self.cycle_completed = False
        self.completed_flag = False
        self.auto_reset_on_next_start = False
        self.structured_data = self._get_structured_data()
        return True

    def reset_for_new_attempt(self):
        return self.reset()

    def mark_for_reset(self):
        self.auto_reset_on_next_start = True

    def check_and_reset_if_needed(self):
        if self.auto_reset_on_next_start:
            self.reset()
            self.auto_reset_on_next_start = False
            return True
        return False

    def _get_state_name(self):
        names = {
            self.STATE_WAITING_FIST: "Ожидание кулака",
            self.STATE_HOLDING_FIST: "Держите кулак",
            self.STATE_WAITING_PALM: "Ожидание ладони",
            self.STATE_HOLDING_PALM: "Держите ладонь",
            self.STATE_COMPLETED: "Упражнение завершено"
        }
        return names.get(self.state, "Неизвестно")

    def _get_state_message(self):
        if self.state == self.STATE_COMPLETED:
            return f"Завершено! {self.total_cycles} циклов"

        next_cycle = min(self.current_cycle + 1, self.total_cycles)

        if self.state == self.STATE_WAITING_FIST:
            return f"Сожмите кулак ({next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_HOLDING_FIST:
            return f"Держите кулак... {self.countdown}с"
        elif self.state == self.STATE_WAITING_PALM:
            return f"Раскройте ладонь ({next_cycle}/{self.total_cycles})"
        elif self.state == self.STATE_HOLDING_PALM:
            return f"Держите ладонь... {self.countdown}с"

        return ""

    def _get_structured_data(self):
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

        if self.state in (self.STATE_HOLDING_FIST, self.STATE_HOLDING_PALM):
            data["countdown"] = self.countdown
            if self.state_start_time:
                elapsed = time.time() - self.state_start_time
                data["progress_percent"] = min(100, (elapsed / self.hold_duration) * 100)

        return data

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        self.check_and_reset_if_needed()

        raised_fingers = sum(finger_states)
        current_time = time.time()

        # Кулак: 0-1 палец поднят, Ладонь: 4-5 пальцев поднято
        is_fist = raised_fingers <= 1
        is_palm = raised_fingers >= 4

        self.cycle_completed = False

        if self.state == self.STATE_WAITING_FIST:
            if is_fist:
                self.state = self.STATE_HOLDING_FIST
                self.state_start_time = current_time
                self.countdown = self.hold_duration

        elif self.state == self.STATE_HOLDING_FIST:
            if not is_fist:
                self.state = self.STATE_WAITING_FIST
            else:
                elapsed = current_time - self.state_start_time
                new_countdown = int(self.hold_duration - elapsed) + 1
                if new_countdown != self.countdown:
                    self.countdown = new_countdown

                if elapsed >= self.hold_duration:
                    self.state = self.STATE_WAITING_PALM
                    self.state_start_time = current_time
                    self.countdown = self.hold_duration

        elif self.state == self.STATE_WAITING_PALM:
            if is_palm:
                self.state = self.STATE_HOLDING_PALM
                self.state_start_time = current_time
                self.countdown = self.hold_duration

        elif self.state == self.STATE_HOLDING_PALM:
            if not is_palm:
                self.state = self.STATE_WAITING_PALM
            else:
                elapsed = current_time - self.state_start_time
                new_countdown = int(self.hold_duration - elapsed) + 1
                if new_countdown != self.countdown:
                    self.countdown = new_countdown

                if elapsed >= self.hold_duration:
                    self.current_cycle += 1
                    self.cycle_completed = True

                    if self.current_cycle >= self.total_cycles:
                        self.state = self.STATE_COMPLETED
                        self.completed_flag = True
                        self.auto_reset_on_next_start = True
                    else:
                        self.state = self.STATE_WAITING_FIST
                        self.state_start_time = current_time
                        self.countdown = self.hold_duration

        self.structured_data = self._get_structured_data()
        return True, self.structured_data["message"]

    def get_finger_colors(self, finger_states):
        """Цвета для пальцев"""
        colors = []
        is_fist_phase = self.state in (self.STATE_WAITING_FIST, self.STATE_HOLDING_FIST)
        is_palm_phase = self.state in (self.STATE_WAITING_PALM, self.STATE_HOLDING_PALM)

        for is_raised in finger_states:
            if is_fist_phase:
                colors.append(self.COLORS['green'] if not is_raised else self.COLORS['red'])
            elif is_palm_phase:
                colors.append(self.COLORS['green'] if is_raised else self.COLORS['red'])
            else:
                colors.append(self.COLORS['gray'])

        return colors

    def draw_feedback(self, frame, finger_states, tip_positions, is_correct, message):
        """МИНИМАЛЬНАЯ отрисовка - только кружки на пальцах и маленькая панель сверху"""
        colors = self.get_finger_colors(finger_states)

        # Только кружки на кончиках пальцев (без текста)
        for i, (x, y) in enumerate(tip_positions):
            color = colors[i] if i < len(colors) else self.COLORS['gray']
            cv2.circle(frame, (x, y), 18, color, -1)
            cv2.circle(frame, (x, y), 18, self.COLORS['white'], 1)

        # МАЛЕНЬКАЯ панель сверху слева (только основная информация)
        cv2.rectangle(frame, (5, 5), (250, 55), self.COLORS['black'], -1)
        cv2.rectangle(frame, (5, 5), (250, 55), self.COLORS['white'], 1)

        # Название упражнения (короткое)
        cv2.putText(frame, "Fist-Palm", (15, 25),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.5, self.COLORS['white'], 1)

        # Прогресс (цикл)
        cv2.putText(frame, f"Cycle: {self.current_cycle}/{self.total_cycles}", (15, 45),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, self.COLORS['green'], 1)

        return frame

    def get_structured_data(self):
        return self.structured_data

    def force_reset_if_needed(self):
        if self.state == self.STATE_COMPLETED or self.completed_flag:
            self.reset()
            return True
        return False