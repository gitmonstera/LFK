# Этот файл делает папку exercises пакетом Python
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

__all__ = [
    'FistExercise',
    'FistIndexExercise',
    'FistPalmExercise',
    'FingerTouchingExercise',
    'EXERCISE_CLASSES'
]