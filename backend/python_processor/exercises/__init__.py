"""
Пакет упражнений для LFK
Содержит все доступные упражнения
"""

import logging

# Настройка логгера для пакета упражнений
logger = logging.getLogger('LFK.Exercises')

from .fist_exercise import FistExercise
from .fist_index_exercise import FistIndexExercise
from .fist_palm_exercise import FistPalmExercise
from .finger_touching_exercise import FingerTouchingExercise

# Словарь для легкого доступа к упражнениям по имени
EXERCISE_CLASSES = {
    "fist": FistExercise,
    "fist-index": FistIndexExercise,
    "fist-palm": FistPalmExercise,
    "finger-touching": FingerTouchingExercise,
}

logger.info(f"Загружено {len(EXERCISE_CLASSES)} упражнений: {', '.join(EXERCISE_CLASSES.keys())}")

__all__ = [
    'FistExercise',
    'FistIndexExercise',
    'FistPalmExercise',
    'FingerTouchingExercise',
    'EXERCISE_CLASSES'
]