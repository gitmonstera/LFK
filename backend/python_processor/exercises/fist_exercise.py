import cv2
import numpy as np
from .base_exercise import BaseExercise

class FistExercise(BaseExercise):
    """Упражнение: Кулак (все пальцы сжаты)"""

    def __init__(self):
        super().__init__()
        self.name = "Кулак"
        self.description = "Сожмите все пальцы в кулак"
        self.exercise_id = "fist"

    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """Проверяет, сжаты ли все пальцы"""
        raised_fingers = sum(finger_states)

        # Для кулака должно быть поднято не более 1 пальца
        is_correct = raised_fingers <= 1

        if is_correct:
            message = "✅ Кулак сжат правильно!"
        else:
            message = f"❌ Сожмите пальцы (поднято {raised_fingers})"

        return is_correct, message

    def get_finger_colors(self, finger_states):
        """Цвета для кулака:
        - Зеленый если палец сжат
        - Красный если палец поднят
        """
        colors = []
        for is_raised in finger_states:
            if is_raised:
                colors.append((0, 0, 255))  # Красный - ошибка (палец поднят)
            else:
                colors.append((0, 255, 0))  # Зеленый - правильно (палец сжат)
        return colors