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
print("Проверяем работу камеры и MediaPipe...")

frame_count = 0

while True:
    good, img = camera.read()
    if not good:
        print("Ошибка захвата кадра")
        break

    frame_count += 1

    # Конвертируем в RGB для MediaPipe
    img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    results = hands.process(img_rgb)

    # Создаем копию для рисования
    display_img = img.copy()

    # Рисуем если есть рука
    if results.multi_hand_landmarks:
        for hand_landmarks in results.multi_hand_landmarks:
            # Рисуем скелет
            mp_drawing.draw_landmarks(
                display_img,
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

                # Рисуем большой круг
                cv2.circle(display_img, (x, y), 20, colors[i], -1)
                cv2.circle(display_img, (x, y), 20, (255, 255, 255), 2)

                # Номер пальца
                cv2.putText(display_img, str(i), (x-10, y-25),
                           cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)

        # Текст об успехе
        cv2.putText(display_img, "РУКА ОБНАРУЖЕНА!", (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 255, 0), 2)
    else:
        cv2.putText(display_img, "НЕТ РУКИ", (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)

    # Добавляем информацию о кадре
    cv2.putText(display_img, f"Кадр: {frame_count}", (10, 60),
               cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

    # Показываем изображение
    cv2.imshow('Direct Test - Hand Detection', display_img)

    # Проверяем, что окно не пустое
    if frame_count == 1:
        print(f"✅ Первый кадр отображен. Размер: {display_img.shape}")

    key = cv2.waitKey(1) & 0xFF
    if key == 27:  # ESC
        break

camera.release()
cv2.destroyAllWindows()
print("Тест завершен")