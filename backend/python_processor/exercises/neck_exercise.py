"""
Упражнение: Наклоны и повороты головы (максимально простая версия)
Без калибровки - просто отслеживаем движение носа
"""

import time
import logging
from typing import Tuple, List, Dict, Any
from collections import deque
from .base_exercise import BaseExercise, BodyPart

logger = logging.getLogger('LFK.Exercise.Neck')

class NeckExercise(BaseExercise):
    """Максимально простое упражнение для шеи"""

    def __init__(self):
        super().__init__()
        self.name = "Наклоны и повороты головы"
        self.description = "Простые движения: вперед, влево, назад, вправо"
        self.exercise_id = "neck"
        self.body_part = BodyPart.POSE

        
        self.movements = [
            {'name': 'Наклон ВПЕРЕД', 'type': 'forward', 'action': 'опустите подбородок'},
            {'name': 'Поворот ВЛЕВО', 'type': 'left', 'action': 'поверните голову влево'},
            {'name': 'Наклон НАЗАД', 'type': 'back', 'action': 'поднимите подбородок'},
            {'name': 'Поворот ВПРАВО', 'type': 'right', 'action': 'поверните голову вправо'}
        ]

        
        self.current_move_idx = 0
        self.current_move = self.movements[self.current_move_idx]

        
        self.current_cycle = 0
        self.total_cycles = 3  

        
        self.hold_start = None
        self.hold_duration = 1.5
        self.is_holding = False

        
        self.base_x = None
        self.base_y = None
        self.is_initialized = False

        
        self.nose_history = deque(maxlen=5)

        
        self.completed = False

        
        self.threshold = 0.008  

        self.structured_data = self._get_structured_data()
        logger.info("Простое упражнение для шеи инициализировано")

    def _get_structured_data(self) -> Dict[str, Any]:
        progress = self.get_progress()

        if self.completed:
            message = "🎉 Упражнение выполнено!"
        elif self.is_holding:
            remaining = max(0, int(self.hold_duration - (time.time() - self.hold_start)) + 1)
            message = f"✅ Держите... {remaining}с"
        else:
            message = f"👉 {self.current_move['action']}"

        return {
            "state": self.current_move['type'],
            "move_name": self.current_move['name'],
            "move_action": self.current_move['action'],
            "current_move": self.current_move_idx + 1,
            "total_moves": len(self.movements),
            "current_cycle": self.current_cycle + 1,
            "total_cycles": self.total_cycles,
            "progress_percent": round(progress, 1),
            "message": message,
            "completed": self.completed,
            "is_holding": self.is_holding,
            "countdown": max(0, int(self.hold_duration - (time.time() - self.hold_start)) + 1) if self.is_holding and self.hold_start else None
        }

    def _get_nose_position(self, landmarks: Dict) -> Tuple[float, float]:
        """Получает позицию носа из landmarks"""
        nose = landmarks.get('nose') or landmarks.get(0)
        if not nose:
            return None, None

        try:
            x = nose.x if hasattr(nose, 'x') else nose[0]
            y = nose.y if hasattr(nose, 'y') else nose[1]
            return x, y
        except:
            return None, None

    def _get_movement(self, x: float, y: float) -> str:
        """Определяет направление движения относительно базовой позиции"""
        if self.base_x is None or self.base_y is None:
            return 'neutral'

        dx = -(x - self.base_x)
        dy = self.base_y - y  

        
        if abs(dy) > abs(dx):
            if dy > self.threshold:
                return 'back'   
            elif dy < -self.threshold:
                return 'forward' 
        else:
            
            if dx > self.threshold:
                return 'right'  
            elif dx < -self.threshold:
                return 'left'   

        return 'neutral'

    def check(self, landmarks: Dict, frame_shape: Tuple[int, int, int]) -> Tuple[bool, str]:
        """Основная логика упражнения"""
        x, y = self._get_nose_position(landmarks)

        if x is None or y is None:
            self.structured_data = self._get_structured_data()
            return False, "❌ Лицо не найдено. Повернитесь к камере"

        
        self.nose_history.append((x, y))
        if len(self.nose_history) >= 3:
            smooth_x = sum(p[0] for p in self.nose_history) / len(self.nose_history)
            smooth_y = sum(p[1] for p in self.nose_history) / len(self.nose_history)
        else:
            smooth_x, smooth_y = x, y

        
        if not self.is_initialized:
            self.base_x = smooth_x
            self.base_y = smooth_y
            self.is_initialized = True
            logger.info(f"Базовое положение установлено: ({self.base_x:.3f}, {self.base_y:.3f})")
            self.structured_data = self._get_structured_data()
            return True, "Готово! Начинайте движения"

        if self.completed:
            return True, "🎉 Упражнение выполнено!"

        
        movement = self._get_movement(smooth_x, smooth_y)
        required = self.current_move['type']

        current_time = time.time()

        
        if self.is_holding:
            elapsed = current_time - self.hold_start
            if elapsed >= self.hold_duration:
                
                self.is_holding = False
                self.current_move_idx += 1

                if self.current_move_idx >= len(self.movements):
                    self.current_move_idx = 0
                    self.current_cycle += 1

                    if self.current_cycle >= self.total_cycles:
                        self.completed = True
                        self.structured_data = self._get_structured_data()
                        return True, "🎉 Поздравляю! Упражнение выполнено!"

                self.current_move = self.movements[self.current_move_idx]
                logger.info(f"Следующее: {self.current_move['name']} (круг {self.current_cycle + 1}/{self.total_cycles})")
            else:
                self.structured_data = self._get_structured_data()
                return True, f"✅ Держите... {int(self.hold_duration - elapsed) + 1}с"
            return True, ""

        
        if movement == required:
            
            self.is_holding = True
            self.hold_start = current_time
            logger.info(f"✅ Правильно! {self.current_move['name']}")
        elif movement != 'neutral':
            
            self.structured_data = self._get_structured_data()
            return False, f"❌ {self.current_move['action']}"

        self.structured_data = self._get_structured_data()
        return True, self.structured_data["message"]

    def get_finger_colors(self, finger_states: List[bool]) -> List[Tuple[int, int, int]]:
        return [(128, 128, 128)] * 5

    def get_progress(self) -> float:
        if self.completed:
            return 100.0

        total = self.total_cycles * len(self.movements)
        done = self.current_cycle * len(self.movements) + self.current_move_idx

        if self.is_holding and self.hold_start:
            elapsed = time.time() - self.hold_start
            done += elapsed / self.hold_duration

        return min(99.0, (done / total) * 100)

    def reset(self) -> bool:
        self.current_move_idx = 0
        self.current_move = self.movements[0]
        self.current_cycle = 0
        self.completed = False
        self.is_holding = False
        self.hold_start = None

        self.is_initialized = False
        self.base_x = None
        self.base_y = None
        self.nose_history.clear()

        self.structured_data = self._get_structured_data()
        logger.info("Упражнение сброшено")
        return True

    def reset_for_new_attempt(self) -> bool:
        return self.reset()

    def get_structured_data(self) -> Dict[str, Any]:
        return self.structured_data