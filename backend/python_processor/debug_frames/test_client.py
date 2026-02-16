import cv2
import websocket
import base64
import json
import numpy as np
import time
import requests
import sys

# URL для разных упражнений
EXERCISE_URLS = {
    '1': "ws://localhost:8080/ws/exercise/fist",        # Кулак
    '2': "ws://localhost:8080/ws/exercise/fist-index",  # Кулак с указательным
    '3': "ws://localhost:8080/ws/exercise/fist-palm",   # Кулак-ладонь
}

EXERCISE_NAMES = {
    '1': "Кулак (все пальцы сжаты)",
    '2': "Кулак с указательным пальцем",
    '3': "Кулак-ладонь (кровообращение)",
}

# Сохраняем токен после логина
auth_token = None
user_info = None

def print_menu():
    """Вывод главного меню"""
    print("\n" + "=" * 60)
    print("🎮 ГЛАВНОЕ МЕНЮ")
    print("=" * 60)
    if auth_token:
        print(f"✅ Авторизован: {user_info.get('username', '')}")
    else:
        print("❌ Не авторизован")
    print("-" * 60)
    print("1 - Войти в систему")
    print("2 - Зарегистрироваться")
    if auth_token:
        print("3 - Выбрать упражнение")
        print("4 - Мой профиль")
    print("q - Выход")
    print("=" * 60)

def print_exercise_menu():
    """Вывод меню упражнений"""
    print("\n" + "=" * 60)
    print("🎮 ВЫБОР УПРАЖНЕНИЯ")
    print("=" * 60)
    for key, name in EXERCISE_NAMES.items():
        print(f"   {key} - {name}")
    print("   b - Назад в главное меню")
    print("=" * 60)

def login():
    """Вход в систему"""
    global auth_token, user_info

    print("\n🔐 ВХОД В СИСТЕМУ")
    email = input("Email: ").strip()
    password = input("Пароль: ").strip()

    try:
        response = requests.post(
            "http://localhost:8080/api/login",
            json={"email": email, "password": password}
        )

        if response.status_code == 200:
            data = response.json()
            auth_token = data["token"]
            user_info = data["user"]
            print(f"✅ Успешный вход! Добро пожаловать, {user_info['username']}!")
            return True
        else:
            error = response.json().get("error", "Unknown error")
            print(f"❌ Ошибка входа: {error}")
            return False

    except requests.exceptions.ConnectionError:
        print("❌ Не удалось подключиться к серверу. Убедитесь, что сервер запущен.")
        return False
    except Exception as e:
        print(f"❌ Ошибка: {e}")
        return False

def register():
    """Регистрация нового пользователя"""
    print("\n📝 РЕГИСТРАЦИЯ НОВОГО ПОЛЬЗОВАТЕЛЯ")
    username = input("Имя пользователя: ").strip()
    email = input("Email: ").strip()
    password = input("Пароль: ").strip()
    first_name = input("Имя (необязательно): ").strip()
    last_name = input("Фамилия (необязательно): ").strip()

    # Формируем запрос
    data = {
        "username": username,
        "email": email,
        "password": password
    }
    if first_name:
        data["first_name"] = first_name
    if last_name:
        data["last_name"] = last_name

    try:
        response = requests.post(
            "http://localhost:8080/api/register",
            json=data
        )

        if response.status_code == 201:
            data = response.json()
            print(f"✅ Регистрация успешна! Теперь можете войти.")
            return True
        else:
            error = response.json().get("error", "Unknown error")
            print(f"❌ Ошибка регистрации: {error}")
            return False

    except requests.exceptions.ConnectionError:
        print("❌ Не удалось подключиться к серверу. Убедитесь, что сервер запущен.")
        return False
    except Exception as e:
        print(f"❌ Ошибка: {e}")
        return False

def get_profile():
    """Получение информации о профиле"""
    if not auth_token:
        print("❌ Не авторизован")
        return

    try:
        response = requests.get(
            "http://localhost:8080/api/profile",
            headers={"Authorization": f"Bearer {auth_token}"}
        )

        if response.status_code == 200:
            profile = response.json()
            print("\n👤 ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ")
            print("=" * 40)
            print(f"ID: {profile.get('id')}")
            print(f"Username: {profile.get('username')}")
            print(f"Email: {profile.get('email')}")
            print(f"Имя: {profile.get('first_name', '')}")
            print(f"Фамилия: {profile.get('last_name', '')}")
            print(f"Дата регистрации: {profile.get('created_at')}")
            print("=" * 40)
        else:
            print(f"❌ Ошибка получения профиля: {response.status_code}")

    except Exception as e:
        print(f"❌ Ошибка: {e}")

def connect_and_run(exercise_key):
    """Подключение и выполнение упражнения с авторизацией"""
    global auth_token

    if not auth_token:
        print("❌ Необходимо войти в систему")
        return False

    url = EXERCISE_URLS[exercise_key]
    exercise_name = EXERCISE_NAMES[exercise_key]

    # Добавляем токен в URL для WebSocket соединения
    ws_url = f"{url}?token={auth_token}"

    print(f"\n🔌 Подключение к {url}...")
    print(f"📋 Упражнение: {exercise_name}")
    print(f"👤 Пользователь: {user_info.get('username')}")

    try:
        # Создаем соединение с токеном
        ws = websocket.create_connection(ws_url, timeout=10)
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
        last_display_time = time.time()

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

                # Отправляем с типом упражнения
                ws.send(json.dumps({
                    "frame": img_base64,
                    "exercise_type": exercise_key
                }))

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

                    # Выводим информацию (раз в секунду)
                    current_time = time.time()
                    if current_time - last_display_time > 1:
                        hand = data.get('hand_detected', False)
                        fingers = data.get('raised_fingers', 0)
                        msg = data.get('message', '')

                        hand_symbol = "🖐️" if hand else "❌"
                        print(f"\r{hand_symbol} | Пальцев: {fingers} | {msg}    ", end="", flush=True)
                        last_display_time = current_time

                except websocket.TimeoutError:
                    pass
                except Exception as e:
                    print(f"\n❌ Ошибка получения: {e}")

            # Добавляем информацию на кадр
            cv2.putText(img, f"User: {user_info.get('username')}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(img, f"Exercise: {exercise_name[:15]}", (10, 55),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(img, "ESC - назад", (10, 80),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

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

    except websocket.WebSocketBadStatusException as e:
        if "401" in str(e):
            print("❌ Ошибка авторизации. Токен недействителен. Пожалуйста, войдите снова.")
            auth_token = None
            user_info = None
        else:
            print(f"\n❌ Ошибка WebSocket: {e}")
        return False
    except Exception as e:
        print(f"\n❌ Ошибка: {e}")
        return False

def main():
    global auth_token, user_info

    print("=" * 60)
    print("🎮 ТЕСТОВЫЙ КЛИЕНТ С АВТОРИЗАЦИЕЙ")
    print("=" * 60)

    while True:
        print_menu()
        choice = input("Выберите действие: ").strip().lower()

        if choice == 'q':
            print("👋 До свидания!")
            break

        elif choice == '1':
            login()

        elif choice == '2':
            register()

        elif choice == '3' and auth_token:
            while True:
                print_exercise_menu()
                ex_choice = input("Выберите упражнение (1-3, b - назад): ").strip().lower()

                if ex_choice == 'b':
                    break

                if ex_choice in EXERCISE_URLS:
                    connect_and_run(ex_choice)
                else:
                    print("❌ Неверный выбор. Попробуйте снова.")

        elif choice == '4' and auth_token:
            get_profile()

        elif choice in ['3', '4'] and not auth_token:
            print("❌ Сначала войдите в систему (пункт 1)")

        else:
            print("❌ Неверный выбор. Попробуйте снова.")

        time.sleep(1)

if __name__ == "__main__":
    main()