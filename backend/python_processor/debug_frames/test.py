import cv2
import websocket
import base64
import json
import numpy as np
import time

# URL для разных упражнений
EXERCISE_URLS = {
    '1': "ws://localhost:8080/ws/exercise/fist",
    '2': "ws://localhost:8080/ws/exercise/fist-index",
}

EXERCISE_NAMES = {
    '1': "Кулак (все пальцы сжаты)",
    '2': "Кулак с указательным пальцем",
}

def print_menu():
    """Вывод меню"""
    print("\n" + "=" * 60)
    print("🎮 ВЫБОР УПРАЖНЕНИЯ")
    print("=" * 60)
    for key, name in EXERCISE_NAMES.items():
        print(f"   {key} - {name}")
    print("   q - Выход")
    print("=" * 60)

def connect_and_run(exercise_key):
    """Подключение и выполнение упражнения"""
    url = EXERCISE_URLS[exercise_key]
    exercise_name = EXERCISE_NAMES[exercise_key]

    print(f"\n🔌 Подключение к {url}...")
    print(f"📋 Упражнение: {exercise_name}")

    try:
        # Создаем соединение
        ws = websocket.create_connection(url, timeout=10)
        print("✅ Подключено!")

        # Открываем камеру
        camera = cv2.VideoCapture(0)
        camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        if not camera.isOpened():
            print("❌ Не удалось открыть камеру")
            return False

        print("📹 Отправка кадров... Нажмите ESC для возврата в меню")
        print("-" * 60)

        frame_count = 0
        fps_time = time.time()

        while True:
            # Читаем кадр
            good, img = camera.read()
            if not good:
                continue

            frame_count += 1

            # Расчет FPS
            if frame_count % 30 == 0:
                current_time = time.time()
                fps = 30 / (current_time - fps_time)
                fps_time = current_time

            # Отправляем каждый 3-й кадр
            if frame_count % 3 == 0:
                _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 50])
                img_base64 = base64.b64encode(buffer).decode('utf-8')

                # Отправляем
                ws.send(json.dumps({"frame": img_base64}))

                # Получаем ответ
                ws.settimeout(0.5)
                try:
                    result = ws.recv()
                    data = json.loads(result)

                    # Показываем обработанный кадр
                    if 'processed_frame' in data and data['processed_frame']:
                        frame_bytes = base64.b64decode(data['processed_frame'])
                        nparr = np.frombuffer(frame_bytes, np.uint8)
                        processed = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                        if processed is not None:
                            cv2.imshow('Processed', processed)

                    # Выводим информацию
                    hand = data.get('hand_detected', False)
                    fingers = data.get('raised_fingers', 0)
                    msg = data.get('message', '')
                    ex_name = data.get('exercise_name', exercise_name)

                    print(f"\r🎯 {ex_name}: {'🖐️' if hand else '❌'} | Пальцев: {fingers} | {msg}    ", end="", flush=True)

                except websocket.TimeoutError:
                    pass
                except Exception as e:
                    print(f"\n❌ Ошибка получения: {e}")

            # Добавляем информацию на кадр
            cv2.putText(img, f"Exercise: {exercise_name[:20]}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)
            cv2.putText(img, "ESC - меню", (10, 60),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (255, 255, 255), 2)

            # Показываем оригинал
            cv2.imshow('Original', img)

            # Выход по ESC
            key = cv2.waitKey(1) & 0xFF
            if key == 27:  # ESC
                break

        camera.release()
        cv2.destroyAllWindows()
        ws.close()
        print("\n🔌 Соединение закрыто")
        return True

    except Exception as e:
        print(f"\n❌ Ошибка: {e}")
        return False

def main():
    print("=" * 60)
    print("🎮 ТЕСТОВЫЙ КЛИЕНТ - МОДУЛЬНЫЕ УПРАЖНЕНИЯ")
    print("=" * 60)

    while True:
        print_menu()
        choice = input("Выберите упражнение (1-2, q - выход): ").strip().lower()

        if choice == 'q':
            print("👋 До свидания!")
            break

        if choice in EXERCISE_URLS:
            connect_and_run(choice)
        else:
            print("❌ Неверный выбор. Попробуйте снова.")

        time.sleep(1)

if __name__ == "__main__":
    main()