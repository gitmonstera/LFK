from abc import ABC, abstractmethod
import cv2
import mediapipe as mp

class BaseExercise(ABC):
    """Базовый класс для всех упражнений"""

    def __init__(self):
        self.name = "Базовое упражнение"
        self.description = ""
        self.exercise_id = "base"

    @abstractmethod
    def check_fingers(self, finger_states, hand_landmarks, frame_shape):
        """
        Проверяет правильность выполнения упражнения
        Должен возвращать (is_correct, message, finger_colors)
        finger_colors - список цветов для каждого пальца (BGR)
        """
        pass

    def get_finger_states(self, hand_landmarks, frame_shape):
        """Получает состояние пальцев (поднят/сжат)"""
        h, w, _ = frame_shape
        finger_tips = [4, 8, 12, 16, 20]
        finger_pips = [3, 6, 10, 14, 18]

        finger_states = []
        tip_positions = []

        for i in range(5):
            tip = hand_landmarks.landmark[finger_tips[i]]
            pip = hand_landmarks.landmark[finger_pips[i]]
            x, y = int(tip.x * w), int(tip.y * h)
            tip_positions.append((x, y))

            if i == 0:  # Большой палец
                index_mcp = hand_landmarks.landmark[5]
                dist = abs(tip.x - index_mcp.x) + abs(tip.y - index_mcp.y)
                finger_states.append(dist > 0.15)
            else:
                finger_states.append(tip.y < pip.y - 0.02)

        return finger_states, tip_positions

    def draw_feedback(self, frame, finger_states, tip_positions, is_correct, message):
        """Рисует обратную связь на кадре"""
        h, w, _ = frame.shape

        # Получаем цвета для пальцев от конкретного упражнения
        finger_colors = self.get_finger_colors(finger_states)

        # Рисуем точки на кончиках пальцев
        for i, (x, y) in enumerate(tip_positions):
            color = finger_colors[i]

            # Рисуем круг
            cv2.circle(frame, (x, y), 20, color, -1)
            cv2.circle(frame, (x, y), 20, (255, 255, 255), 2)

            # Номер пальца и статус
            status = "⬆️" if finger_states[i] else "⬇️"
            cv2.putText(frame, f"{i}{status}", (x-20, y-25),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        # Информация на кадре
        cv2.rectangle(frame, (5, 5), (450, 130), (0, 0, 0), -1)
        cv2.rectangle(frame, (5, 5), (450, 130), (255, 255, 255), 2)

        info_y = 30
        cv2.putText(frame, f"Упражнение: {self.name}", (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        info_y += 25
        cv2.putText(frame, f"Пальцев поднято: {sum(finger_states)}/5", (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        info_y += 25
        status_text = " ".join([f"{i}:{'⬆️' if s else '⬇️'}" for i, s in enumerate(finger_states)])
        cv2.putText(frame, status_text, (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

        info_y += 25
        color = (0, 255, 0) if is_correct else (0, 0, 255)
        cv2.putText(frame, message, (15, info_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)

        return frame

    @abstractmethod
    def get_finger_colors(self, finger_states):
        """
        Возвращает цвета для каждого пальца
        Должен возвращать список из 5 цветов в формате BGR
        """
        pass