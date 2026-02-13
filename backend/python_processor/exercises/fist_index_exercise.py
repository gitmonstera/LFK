import cv2
import numpy as np
from .base_exercise import BaseExercise

class FistIndexExercise(BaseExercise):
    """Упражнение: Кулак с поднятым указательным пальцем"""

    def __init__(self):
        super().__init__()
        self.name = "Кулак с указательным"
        self.description = "Сожмите кулак, но указательный палец поднимите"
        self.exercise_id = "fist-index"

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """Проверяет, поднят ли указательный и сжаты ли остальные"""
        index_raised = finger_states[1]  # Указательный палец (индекс 1)
        other_raised = any([finger_states[2], finger_states[3], finger_states[4]])

        is_correct = index_raised and not other_raised

        if is_correct:
            message = "✅ Указательный поднят, остальные сжаты!"
        elif not index_raised:
            message = "❌ Поднимите указательный палец"
        elif other_raised:
            message = "❌ Сожмите остальные пальцы"
        else:
            message = "❌ Неправильное положение"

        return is_correct, message

    def get_finger_colors(self, finger_states):
        """Цвета для упражнения:
        - Указательный палец (i=1): зеленый если поднят, красный если сжат
        - Остальные пальцы: зеленый если сжаты, красный если подняты
        """
        colors = []
        for i, is_raised in enumerate(finger_states):
            if i == 1:  # Указательный палец
                if is_raised:
                    colors.append((0, 255, 0))  # Зеленый - правильно (поднят)
                else:
                    colors.append((0, 0, 255))  # Красный - ошибка (сжат)
            else:  # Остальные пальцы
                if is_raised:
                    colors.append((0, 0, 255))  # Красный - ошибка (поднят)
                else:
                    colors.append((0, 255, 0))  # Зеленый - правильно (сжат)
        return colors