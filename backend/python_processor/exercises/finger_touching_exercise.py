"""
Упражнение "Считалочка" - поочередное касание пальцев с большим (УЛЬТРА-ОПТИМИЗИРОВАННО)
"""

import time
import logging
from .base_exercise import BaseExercise
import cv2

logger = logging.getLogger('LFK.Exercise.FingerTouching')

class FingerTouchingExercise(BaseExercise):
    """Упражнение: Поочередное касание пальцев - с адаптивным порогом"""

    # Константы состояний
    STATE_WAITING = "waiting"
    STATE_HOLDING = "holding"
    STATE_COMPLETED = "completed"

    # Предопределенные индексы (кэш)
    FINGER_TIPS = (4, 8, 12, 16, 20)
    FINGER_MCP = (1, 5, 9, 13, 17)  # суставы для определения толщины
    FINGER_NAMES = ("указательным", "средним", "безымянным", "мизинцем")

    # Цвета (BGR)
    COLORS = {
        'yellow': (0, 255, 255),
        'green': (0, 255, 0),
        'cyan': (255, 255, 0),
        'gray': (128, 128, 128),
        'red': (0, 0, 255)
    }

    def __init__(self):
        super().__init__()
        self.name = "Считалочка"
        self.description = "Поочередное касание пальцев - развивает мелкую моторику"
        self.exercise_id = "finger-touching"

        # Состояние
        self.state = self.STATE_WAITING
        self.hold_start = 0.0
        self.hold_duration = 0.7

        # Счетчики
        self.current_cycle = 0
        self.current_finger = 0
        self.total_cycles = 5

        # Флаги
        self.completed = False
        self.auto_reset = False
        self.cycle_completed = False

        # Для касания - адаптивные пороги
        self.base_threshold = 0.045
        self.touch_cooldown = 0.2
        self.last_touch_time = 0.0

        # Для адаптивного порога (учитываем толщину пальцев)
        self.finger_sizes = [0.0] * 5  # размеры пальцев
        self.calibrated = False
        self.calibration_start = 0.0
        self.calibration_duration = 1.5

        self.structured_data = self._get_structured_data()
        logger.info(f"Упражнение инициализировано: {self.name}")

    def reset(self):
        """Быстрый сброс"""
        self.state = self.STATE_WAITING
        self.current_cycle = 0
        self.current_finger = 0
        self.completed = False
        self.auto_reset = False
        self.cycle_completed = False
        self.hold_start = 0.0
        self.last_touch_time = 0.0
        self.calibrated = False
        self.finger_sizes = [0.0] * 5
        self.structured_data = self._get_structured_data()
        return True

    def _calibrate_finger_sizes(self, hand_landmarks):
        """Калибровка размеров пальцев пользователя"""
        if not self.calibrated:
            if self.calibration_start == 0.0:
                self.calibration_start = time.time()
                return False

            if time.time() - self.calibration_start >= self.calibration_duration:
                # Калибровка завершена
                self.calibrated = True
                # Усредняем и корректируем пороги
                avg_size = sum(self.finger_sizes) / 5 if any(self.finger_sizes) else 0.045
                self.touch_threshold = avg_size * 0.8  # 80% от среднего размера
                logger.info(f"Калибровка завершена, порог: {self.touch_threshold:.3f}")
                return True
            return False

        # Собираем данные о размерах пальцев
        for i in range(5):
            tip = hand_landmarks.landmark[self.FINGER_TIPS[i]]
            mcp = hand_landmarks.landmark[self.FINGER_MCP[i]]
            # Размер пальца = расстояние от кончика до сустава
            size = abs(tip.y - mcp.y)
            # Экспоненциальное сглаживание
            self.finger_sizes[i] = self.finger_sizes[i] * 0.7 + size * 0.3

        return False

    def get_finger_states(self, hand_landmarks, frame_shape):
        """Быстрое получение позиций пальцев"""
        h, w, _ = frame_shape
        tip_positions = []
        finger_states = [False] * 5

        # Калибровка
        self._calibrate_finger_sizes(hand_landmarks)

        for i in range(5):
            tip = hand_landmarks.landmark[self.FINGER_TIPS[i]]
            x = int(tip.x * w)
            y = int(tip.y * h)
            tip_positions.append((x, y))

            if i == 0:
                index_mcp = hand_landmarks.landmark[5]
                dist = abs(tip.x - index_mcp.x) + abs(tip.y - index_mcp.y)
                finger_states[i] = dist > 0.15
            else:
                pip = hand_landmarks.landmark[self.FINGER_TIPS[i] - 1]
                finger_states[i] = tip.y < pip.y - 0.02

        return finger_states, tip_positions

    def reset_for_new_attempt(self):
        return self.reset()

    def mark_for_reset(self):
        self.auto_reset = True

    def _get_progress_percent(self):
        total_touches = self.current_cycle * 4 + self.current_finger
        total_needed = self.total_cycles * 4
        return int((total_touches / total_needed) * 100) if total_needed else 0

    def _get_state_message(self):
        if self.completed:
            return f"🎉 Упражнение завершено! {self.total_cycles} циклов"

        next_cycle = min(self.current_cycle + 1, self.total_cycles)

        if not self.calibrated:
            return f"🔧 Калибровка... держите пальцы раскрытыми ({int(self.calibration_duration - (time.time() - self.calibration_start))}с)"

        if self.state == self.STATE_WAITING:
            if self.current_finger == 0:
                return f"Коснитесь указательным пальцем (цикл {next_cycle}/{self.total_cycles})"
            return f"Коснитесь {self.FINGER_NAMES[self.current_finger]} пальцем (цикл {next_cycle}/{self.total_cycles})"
        else:
            remaining = int(self.hold_duration - (time.time() - self.hold_start)) + 1
            if self.current_finger == 0:
                return f"Держите... {remaining}с (цикл {next_cycle}/{self.total_cycles})"
            return f"Держите... {remaining}с"

    def _get_structured_data(self):
        data = {
            "state": self.state,
            "current_cycle": self.current_cycle,
            "total_cycles": self.total_cycles,
            "current_finger": self.current_finger,
            "progress_percent": self._get_progress_percent(),
            "message": self._get_state_message(),
            "cycle_completed": self.cycle_completed,
            "completed": self.completed,
            "auto_reset": self.auto_reset,
            "calibrated": self.calibrated
        }

        if self.state == self.STATE_HOLDING and self.hold_start:
            elapsed = time.time() - self.hold_start
            remaining = int(self.hold_duration - elapsed) + 1
            data["countdown"] = remaining
            data["hold_progress"] = min(100, (elapsed / self.hold_duration) * 100)

        return data

    def get_finger_colors(self, finger_states):
        """Быстрые цвета для пальцев"""
        colors = []
        target = self.current_finger + 1
        is_holding = self.state == self.STATE_HOLDING

        for i in range(5):
            if i == 0:
                colors.append(self.COLORS['yellow'])
            elif i < target:
                colors.append(self.COLORS['green'])
            elif i == target:
                colors.append(self.COLORS['green'] if is_holding else self.COLORS['cyan'])
            else:
                colors.append(self.COLORS['gray'])
        return colors

    def _get_adaptive_threshold(self, finger_idx):
        """Возвращает адаптивный порог для конкретного пальца"""
        if self.calibrated and self.finger_sizes[finger_idx] > 0:
            # Порог = 60% от размера пальца (но не меньше базового)
            return max(self.base_threshold, self.finger_sizes[finger_idx] * 0.6)
        return self.base_threshold

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """Ультра-быстрая проверка касаний с адаптивным порогом"""
        # Автосброс
        if self.auto_reset:
            self.reset()
            self.auto_reset = False

        if self.completed:
            self.structured_data = self._get_structured_data()
            return True, self.structured_data["message"]

        # Если калибровка не завершена
        if not self.calibrated:
            self.structured_data = self._get_structured_data()
            return True, self.structured_data["message"]

        current_time = time.time()

        # Быстрое получение координат
        thumb = hand_landmarks.landmark[self.FINGER_TIPS[0]]
        target_idx = self.current_finger + 1
        target = hand_landmarks.landmark[self.FINGER_TIPS[target_idx]]

        # Манхэттенское расстояние
        dx = thumb.x - target.x
        dy = thumb.y - target.y
        distance = abs(dx) + abs(dy)

        # Адаптивный порог для текущего пальца
        threshold = self._get_adaptive_threshold(target_idx)

        # Дополнительная проверка: учитываем также относительное расстояние
        # Нормализуем по размеру пальца
        finger_size = self.finger_sizes[target_idx]
        normalized_distance = distance / max(finger_size, 0.01)
        is_touching = distance < threshold or normalized_distance < 0.5

        self.cycle_completed = False

        if self.state == self.STATE_WAITING:
            if is_touching and (current_time - self.last_touch_time) > self.touch_cooldown:
                self.state = self.STATE_HOLDING
                self.hold_start = current_time
                self.last_touch_time = current_time

        elif self.state == self.STATE_HOLDING:
            # Для отпускания используем чуть больший порог (гистерезис)
            release_threshold = threshold * 1.3
            if distance >= release_threshold:
                self.state = self.STATE_WAITING
            elif current_time - self.hold_start >= self.hold_duration:
                self.state = self.STATE_WAITING
                self.current_finger += 1
                self.cycle_completed = True

                if self.current_finger >= 4:
                    self.current_finger = 0
                    self.current_cycle += 1

                    if self.current_cycle >= self.total_cycles:
                        self.completed = True
                        self.auto_reset = True
                        logger.info("Упражнение завершено!")

        self.structured_data = self._get_structured_data()
        return True, self.structured_data["message"]

    def draw_feedback(self, frame, finger_states, tip_positions, is_correct, message):
        """Быстрая отрисовка с большими точками"""
        colors = self.get_finger_colors(finger_states)

        # Рисуем большие кружки
        for i, (x, y) in enumerate(tip_positions):
            color = colors[i] if i < len(colors) else self.COLORS['gray']
            # Радиус зависит от размера пальца (для наглядности)
            radius = 20 + int(self.finger_sizes[i] * 50) if self.calibrated else 22
            radius = min(radius, 30)  # ограничиваем максимум
            cv2.circle(frame, (x, y), radius, color, -1)
            cv2.circle(frame, (x, y), radius, (255, 255, 255), 2)
            cv2.putText(frame, str(i + 1), (x - 8, y + 8),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        # Информационная панель
        h, w = frame.shape[:2]
        cv2.rectangle(frame, (5, 5), (400, 95), (0, 0, 0), -1)
        cv2.putText(frame, self.name[:12], (15, 28),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1)

        # Статус калибровки
        if not self.calibrated:
            cv2.putText(frame, "КАЛИБРОВКА...", (15, 48),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, (0, 255, 255), 1)
        else:
            # Прогресс
            progress = self._get_progress_percent()
            bar_width = int(progress / 100 * 250)
            cv2.rectangle(frame, (15, 40), (15 + bar_width, 52), (0, 255, 0), -1)
            cv2.putText(frame, f"{progress}%", (280, 50),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.45, (255, 255, 255), 1)

        # Сообщение
        msg = message[:35]
        cv2.putText(frame, msg, (15, 75),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, (200, 200, 200), 1)

        return frame

    def get_structured_data(self):
        return self.structured_data

    def force_reset_if_needed(self):
        if self.completed:
            self.reset()
            return True
        return False