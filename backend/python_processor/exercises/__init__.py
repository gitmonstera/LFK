# Этот файл делает папку exercises пакетом Python
from .fist_exercise import FistExercise
from .fist_index_exercise import FistIndexExercise
from .fist_palm_exercise import FistPalmExercise

# Словарь для легкого доступа к упражнениям по имени
EXERCISE_CLASSES = {
    "fist": FistExercise,
    "fist-index": FistIndexExercise,
    "fist-palm": FistPalmExercise,
}

__all__ = ['FistExercise', 'FistIndexExercise', 'FistPalmExercise', 'EXERCISE_CLASSES']