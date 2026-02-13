# Этот файл делает папку exercises пакетом Python
from .fist_exercise import FistExercise
from .fist_index_exercise import FistIndexExercise

# Словарь для легкого доступа к упражнениям по имени
EXERCISE_CLASSES = {
    "fist": FistExercise,
    "fist-index": FistIndexExercise,
}

__all__ = ['FistExercise', 'FistIndexExercise', 'EXERCISE_CLASSES']