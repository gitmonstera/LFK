import cv2
import mediapipe as mp
import numpy as np

# Инициализация MediaPipe
mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils
hands = mp_hands.Hands(
    static_image_mode=False,
    max_num_hands=1,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5
)

# Захват видео
camera = cv2.VideoCapture(0)
camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

print("Прямой тест MediaPipe. Нажмите ESC для выхода.")

while True:
    good, img = camera.read()
    if not good:
        break

    # Конвертируем в RGB
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    # Обрабатываем
    results = hands.process(img_rgb)

    # Рисуем landmarks если есть
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            mp_drawing.draw_landmarks(
                img,
                hand_landmarks,
                mp_hands.HAND_CONNECTIONS
            )

            # Рисуем точки на кончиках пальцев
            h, w, _ = img.shape
            finger_tips = [4, 8, 12, 16, 20]
            colors = [(255, 0, 255), (255, 0, 0), (0, 255, 0), (0, 255, 255), (0, 0, 255)]

            for i, tip_id in enumerate(finger_tips):
                lm = hand_landmarks.landmark[tip_id]
                x, y = int(lm.x * w), int(lm.y * h)
                cv2.circle(img, (x, y), 15, colors[i], -1)
                cv2.circle(img, (x, y), 15, (255, 255, 255), 2)
                cv2.putText(img, str(i), (x-10, y-20), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        cv2.putText(img, "Рука обнаружена!", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
    else:
        cv2.putText(img, "Рука не обнаружена", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

    cv2.imshow('Direct MediaPipe Test', img)

    if cv2.waitKey(1) & 0xFF == 27:  # ESC
        break

camera.release()
cv2.destroyAllWindows()