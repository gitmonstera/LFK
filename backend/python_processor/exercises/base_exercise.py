from abc import ABC, abstractmethod
import cv2
import logging
import numpy as np
from typing import List, Tuple, Dict, Any, Optional
from dataclasses import dataclass, field
from enum import Enum

# Настройка логгера
logger = logging.getLogger('LFK.Exercises.Base')


class BodyPart(Enum):
    HAND = "hand"
    POSE = "pose"
    LEG = "leg"
    HEAD = "head"
    ELBOW = "elbow"
    KNEE = "knee"
    FOOT = "foot"
    SHOULDER = "shoulder"  # добавь эту строку


@dataclass
class LandmarkPoint:
    """Точка с координатами (оптимизированная)"""
    x: float
    y: float
    z: float = 0.0
    visibility: float = 1.0

    def distance_to(self, other: 'LandmarkPoint') -> float:
        dx = self.x - other.x
        dy = self.y - other.y
        dz = self.z - other.z
        return (dx*dx + dy*dy + dz*dz) ** 0.5


class BaseExercise(ABC):
    """
    Базовый класс для всех упражнений
    Поддерживает как старый интерфейс (check_fingers), так и новый (check)
    """

    # Предопределенные цвета
    COLORS = {
        'green': (0, 255, 0),
        'red': (0, 0, 255),
        'blue': (255, 0, 0),
        'yellow': (0, 255, 255),
        'white': (255, 255, 255),
        'black': (0, 0, 0),
        'gray': (128, 128, 128),
        'cyan': (255, 255, 0),
        'magenta': (255, 0, 255)
    }

    def __init__(self):
        self.name = "Базовое упражнение"
        self.description = ""
        self.exercise_id = "base"
        self.body_part = BodyPart.HAND

        self.logger = logging.getLogger(f'LFK.Exercise.{self.__class__.__name__}')
        self._debug_mode = False
        self._frame_counter = 0
        self._feedback_panel_rect = (5, 5, 450, 130)

        self.logger.debug(f"Инициализация {self.name}")

    # ============ НОВЫЙ ИНТЕРФЕЙС (для упражнений на тело) ============

    def check(self, landmarks: Dict[str, Any], frame_shape: Tuple[int, int, int]) -> Tuple[bool, str]:
        """
        Проверяет правильность выполнения упражнения (новый интерфейс)
        По умолчанию вызывает старый метод check_fingers если есть hand_landmarks
        """
        # Если есть hand_landmarks и есть старый метод - используем его
        if 'hand' in landmarks and hasattr(self, 'check_fingers'):
            hand_landmarks = landmarks['hand']
            tip_positions = landmarks.get('tip_positions')

            # Получаем finger_states
            if hasattr(self, 'get_finger_states'):
                finger_states, _ = self.get_finger_states(hand_landmarks, frame_shape)
            else:
                finger_states = [False] * 5

            return self.check_fingers(finger_states, hand_landmarks, frame_shape)

        # Если нет - возвращаем заглушку
        return False, "Метод check не реализован"

    # ============ СТАРЫЙ ИНТЕРФЕЙС (для обратной совместимости) ============

    def check_fingers(self, finger_states: List[bool], hand_landmarks, frame_shape: Tuple[int, int, int]) -> Tuple[bool, str]:
        """
        Проверяет правильность выполнения упражнения (старый интерфейс)
        По умолчанию вызывает новый метод check
        """
        # Создаем словарь landmarks для нового интерфейса
        landmarks = {'hand': hand_landmarks}
        if hasattr(self, 'get_finger_states'):
            finger_states, tip_positions = self.get_finger_states(hand_landmarks, frame_shape)
            landmarks['tip_positions'] = tip_positions
            landmarks['finger_states'] = finger_states

        return self.check(landmarks, frame_shape)

    def get_finger_states(self, hand_landmarks, frame_shape: Tuple[int, int, int]) -> Tuple[List[bool], List[Tuple[int, int]]]:
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

    def draw_feedback(self, frame, finger_states: List[bool], tip_positions: List[Tuple[int, int]],
                      is_correct: bool, message: str) -> np.ndarray:
        """Рисует обратную связь на кадре"""
        h, w, _ = frame.shape
        finger_colors = self.get_finger_colors(finger_states)

        # Рисуем точки на кончиках пальцев
        for i, (x, y) in enumerate(tip_positions):
            color = finger_colors[i] if i < len(finger_colors) else self.COLORS['gray']
            cv2.circle(frame, (x, y), 20, color, -1)
            cv2.circle(frame, (x, y), 20, self.COLORS['white'], 2)
            status = "⬆️" if finger_states[i] else "⬇️"
            cv2.putText(frame, f"{i}{status}", (x-20, y-25),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, self.COLORS['white'], 2)

        # Информационная панель
        self._draw_info_panel(frame, is_correct, message, sum(finger_states))

        return frame

    def _draw_info_panel(self, frame, is_correct: bool, message: str, fingers_up: int):
        """Рисует информационную панель"""
        x, y, w, h = self._feedback_panel_rect

        cv2.rectangle(frame, (x, y), (x + w, y + h), self.COLORS['black'], -1)
        cv2.rectangle(frame, (x, y), (x + w, y + h), self.COLORS['white'], 2)

        text_y = y + 30
        cv2.putText(frame, f"Exercise: {self.name[:25]}", (x + 10, text_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, self.COLORS['white'], 2)

        text_y += 25
        cv2.putText(frame, f"Fingers up: {fingers_up}/5", (x + 10, text_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, self.COLORS['white'], 2)

        text_y += 25
        color = self.COLORS['green'] if is_correct else self.COLORS['red']
        msg = message[:40] + "..." if len(message) > 40 else message
        cv2.putText(frame, msg, (x + 10, text_y),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.6, color, 2)

    # ============ АБСТРАКТНЫЕ МЕТОДЫ ============

    @abstractmethod
    def get_finger_colors(self, finger_states: List[bool]) -> List[Tuple[int, int, int]]:
        """Возвращает цвета для каждого пальца"""
        pass

    # ============ ОПЦИОНАЛЬНЫЕ МЕТОДЫ ============

    def get_structured_data(self) -> Optional[Dict[str, Any]]:
        """Возвращает структурированные данные для клиента"""
        return None

    def reset(self) -> bool:
        """Сбрасывает упражнение"""
        return True

    def reset_for_new_attempt(self) -> bool:
        """Сбрасывает для нового подхода"""
        return self.reset()

    def mark_for_reset(self):
        """Помечает для сброса при следующем запуске"""
        pass

    def get_progress(self) -> float:
        """Возвращает прогресс выполнения (0-100)"""
        return 0.0