import cv2
import websocket
import base64
import json
import numpy as np
import time
import requests
import urllib.parse

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

EXERCISE_TYPES = {
    '1': 'fist',
    '2': 'fist-index',
    '3': 'fist-palm',
}

# Глобальные переменные
auth_token = None
user_info = None

def print_menu():
    """Вывод главного меню"""
    global user_info
    print("\n" + "=" * 60)
    print("🎮 ГЛАВНОЕ МЕНЮ")
    print("=" * 60)
    if auth_token and user_info:
        print(f"✅ Авторизован: {user_info.get('username', '')}")
    else:
        print("❌ Не авторизован")
    print("-" * 60)
    print("1 - Войти в систему")
    print("2 - Зарегистрироваться")
    if auth_token and user_info:
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
            print(f"🔑 Токен получен (первые 20 символов): {auth_token[:20]}...")
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
    global auth_token

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
        return False

def display_fist_palm_progress(data):
    """Отображает прогресс для упражнения Кулак-ладонь"""
    if 'structured' not in data:
        print("❌ structured data отсутствует в ответе!")
        return

    structured = data['structured']

    # Если structured данные пустые или None
    if not structured or all(v is None for v in structured.values()):
        print("\n⚠️ Structured данные пусты (сервер не инициализировал состояние)")
        return

    # Безопасно получаем данные с проверкой на None и тип
    state = structured.get('state')
    # Если state не строка или None, преобразуем в строку
    if state is None:
        state = 'unknown'
    elif not isinstance(state, str):
        state = str(state)

    state_name = structured.get('state_name') or ''
    if not isinstance(state_name, str):
        state_name = str(state_name)

    current_cycle = structured.get('current_cycle')
    if current_cycle is None:
        current_cycle = 0
    else:
        try:
            current_cycle = int(current_cycle)
        except:
            current_cycle = 0

    total_cycles = structured.get('total_cycles') or 5
    try:
        total_cycles = int(total_cycles)
    except:
        total_cycles = 5

    countdown = structured.get('countdown')
    if countdown is not None:
        try:
            countdown = int(countdown)
        except:
            countdown = None

    progress = structured.get('progress_percent') or 0
    try:
        progress = float(progress)
    except:
        progress = 0

    message = structured.get('message', data.get('message', ''))
    if not isinstance(message, str):
        message = str(message)

    # Очищаем экран для отображения прогресса
    print("\033[2J\033[H", end="")  # Очистка экрана
    print("=" * 60)
    print(f"🎯 {EXERCISE_NAMES['3']}")
    print("=" * 60)

    # Статус руки
    hand = data.get('hand_detected', False)
    hand_symbol = "🖐️" if hand else "❌"
    print(f"{hand_symbol} Рука: {'в кадре' if hand else 'не обнаружена'}")

    # Состояние пальцев
    finger_states = data.get('finger_states', [])
    if finger_states:
        finger_names = ["Большой", "Указат", "Средний", "Безым", "Мизинец"]
        finger_status = []
        for i, state in enumerate(finger_states):
            if state:
                finger_status.append(f"{finger_names[i]}:⬆️")
            else:
                finger_status.append(f"{finger_names[i]}:⬇️")
        print(f"🖐️ Пальцы: {', '.join(finger_status)}")

    print("-" * 60)

    # Отображение шагов для Кулак-ладонь
    steps = [
        {"name": "Сожмите кулак", "state": "waiting_fist"},
        {"name": "Держите кулак", "state": "holding_fist"},
        {"name": "Раскройте ладонь", "state": "waiting_palm"},
        {"name": "Держите ладонь", "state": "holding_palm"}
    ]

    print("📋 ПРОГРЕСС УПРАЖНЕНИЯ:")

    # Определяем текущий шаг
    current_step_index = -1
    for i, step in enumerate(steps):
        if step["state"] == state:
            current_step_index = i
            break

    # Отображаем все шаги
    for i, step in enumerate(steps):
        if i < current_step_index:
            # Пройденные шаги
            print(f"  ✅ {step['name']}")
        elif i == current_step_index:
            # Текущий шаг
            if "holding" in str(state) and countdown is not None:
                print(f"  ⏳ {step['name']} [{countdown}с]")
            else:
                print(f"  ⏳ {step['name']}")
        else:
            # Будущие шаги
            print(f"  ⬜ {step['name']}")

    print(f"\n🔄 Цикл: {current_cycle}/{total_cycles}")

    # Прогресс-бар для удержания
    if "holding" in str(state) and countdown is not None:
        bar_length = 30
        filled = int(progress / 100 * bar_length)
        bar = "█" * filled + "░" * (bar_length - filled)
        print(f"\n⏱️  Осталось: {countdown}с [{bar}] {progress:.0f}%")

    print(f"\n📢 {message}")
    print("-" * 60)

def display_regular_exercise(data, exercise_name):
    """Отображает обычное упражнение"""
    print("\033[2J\033[H", end="")  # Очистка экрана
    print("=" * 60)
    print(f"🎯 {exercise_name}")
    print("=" * 60)

    hand = data.get('hand_detected', False)
    fingers = data.get('raised_fingers', 0)
    msg = data.get('message', '')

    hand_symbol = "🖐️" if hand else "❌"
    print(f"{hand_symbol} Рука: {'в кадре' if hand else 'не обнаружена'}")
    print(f"👆 Пальцев поднято: {fingers}/5")

    finger_states = data.get('finger_states', [])
    if finger_states:
        finger_names = ["Б", "У", "С", "Бз", "М"]
        status = []
        for i, state in enumerate(finger_states):
            if state:
                status.append(f"{finger_names[i]}⬆️")
            else:
                status.append(f"{finger_names[i]}⬇️")
        print(f"🖐️ {' | '.join(status)}")

    print(f"\n📢 {msg}")
    print("-" * 60)

def connect_and_run(exercise_key):
    """Подключение и выполнение упражнения с авторизацией"""
    global auth_token, user_info

    if not auth_token or not user_info:
        print("❌ Необходимо войти в систему")
        return False

    url = EXERCISE_URLS[exercise_key]
    exercise_name = EXERCISE_NAMES[exercise_key]
    exercise_type = EXERCISE_TYPES[exercise_key]

    # Кодируем токен для URL
    encoded_token = urllib.parse.quote(auth_token)
    ws_url = f"{url}?token={encoded_token}"

    print(f"\n🔌 Подключение к WebSocket...")
    print(f"📋 Упражнение: {exercise_name}")
    print(f"🔑 Тип: {exercise_type}")
    print(f"👤 Пользователь: {user_info.get('username')}")

    try:
        ws = websocket.create_connection(ws_url, timeout=10)
        print("✅ WebSocket подключен успешно!")

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
        last_update_time = time.time()

        while True:
            good, img = camera.read()
            if not good:
                continue

            frame_count += 1

            if frame_count % 30 == 0:
                current_time = time.time()
                fps = 30 / (current_time - fps_time)
                fps_time = current_time

            if frame_count % 3 == 0:
                _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 50])
                img_base64 = base64.b64encode(buffer).decode('utf-8')

                ws.send(json.dumps({
                    "frame": img_base64,
                    "exercise_type": exercise_type
                }))

                ws.settimeout(0.5)
                try:
                    result = ws.recv()
                    data = json.loads(result)

                    # ВСЕГДА печатаем что пришло от сервера
                    print("\n" + "=" * 60)
                    print("📦 ПОЛНЫЙ ОТВЕТ ОТ СЕРВЕРА:")
                    print(json.dumps(data, indent=2, ensure_ascii=False))
                    print("=" * 60)

                    if 'processed_frame' in data and data['processed_frame']:
                        frame_bytes = base64.b64decode(data['processed_frame'])
                        nparr = np.frombuffer(frame_bytes, np.uint8)
                        processed = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                        if processed is not None:
                            cv2.imshow('Processed', processed)

                    # Обновляем отображение
                    current_time = time.time()
                    if current_time - last_update_time > 0.5:
                        if exercise_key == '3':  # Кулак-ладонь
                            display_fist_palm_progress(data)
                        else:
                            display_regular_exercise(data, exercise_name)
                        last_update_time = current_time

                except websocket.WebSocketTimeoutException:
                    pass
                except Exception as e:
                    print(f"\n❌ Ошибка получения: {e}")
                    import traceback
                    traceback.print_exc()

            # Добавляем информацию на кадр
            cv2.putText(img, f"User: {user_info.get('username', '')}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(img, f"Ex: {exercise_type}", (10, 55),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 2)
            cv2.putText(img, "ESC - назад", (10, 80),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

            cv2.imshow('Original', img)

            key = cv2.waitKey(1) & 0xFF
            if key == 27:  # ESC
                break

        camera.release()
        cv2.destroyAllWindows()
        ws.close()
        print("\n🔌 Соединение закрыто")
        return True

    except websocket.WebSocketBadStatusException as e:
        status = str(e)
        if "401" in status:
            print(f"\n❌ Ошибка авторизации (401). Токен недействителен.")
            auth_token = None
            user_info = None
        else:
            print(f"\n❌ Ошибка WebSocket: {e}")
        return False
    except Exception as e:
        print(f"\n❌ Ошибка: {e}")
        import traceback
        traceback.print_exc()
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

        elif choice == '3' and auth_token and user_info:
            while True:
                print_exercise_menu()
                ex_choice = input("Выберите упражнение (1-3, b - назад): ").strip().lower()

                if ex_choice == 'b':
                    break

                if ex_choice in EXERCISE_URLS:
                    connect_and_run(ex_choice)
                else:
                    print("❌ Неверный выбор. Попробуйте снова.")

        elif choice == '4' and auth_token and user_info:
            get_profile()

        elif choice in ['3', '4'] and (not auth_token or not user_info):
            print("❌ Сначала войдите в систему (пункт 1)")

        else:
            print("❌ Неверный выбор. Попробуйте снова.")

        time.sleep(1)

if __name__ == "__main__":
    main()