import base64
import json
import time
import urllib.parse
from datetime import datetime

import cv2
import numpy as np
import requests
import websocket

# ==================== НАСТРОЙКИ ====================
# Меняй только здесь!
SERVER_IP = "localhost"  # IP адрес сервера
SERVER_PORT = "8080"          # порт сервера

# Собираем URL из настроек
BASE_URL = f"http://{SERVER_IP}:{SERVER_PORT}"
WS_BASE_URL = f"ws://{SERVER_IP}:{SERVER_PORT}"

EXERCISE_URLS = {
    '1': f"{WS_BASE_URL}/ws/exercise/fist",
    '2': f"{WS_BASE_URL}/ws/exercise/fist-index",
    '3': f"{WS_BASE_URL}/ws/exercise/fist-palm",
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

# Таймауты (в секундах)
CONNECTION_TIMEOUT = 10
REQUEST_TIMEOUT = 5
WS_TIMEOUT = 0.5

# Настройки камеры
FRAME_WIDTH = 640
FRAME_HEIGHT = 480
JPEG_QUALITY = 50
FRAME_SKIP = 3  # отправлять каждый 3-й кадр
# ===================================================

auth_token = None
user_info = None


class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    CYAN = '\033[96m'
    MAGENTA = '\033[95m'
    END = '\033[0m'
    BOLD = '\033[1m'

def clear_screen():
    """Очистка экрана"""
    print("\033[2J\033[H", end="")

def print_header(text):
    """Печать заголовка"""
    print(f"\n{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{text}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")

def print_menu():
    """Вывод главного меню"""
    clear_screen()
    print_header("🎮 ГЛАВНОЕ МЕНЮ")

    if auth_token and user_info:
        print(f"{Colors.GREEN}✅ Авторизован: {user_info.get('username', '')}{Colors.END}")
    else:
        print(f"{Colors.RED}❌ Не авторизован{Colors.END}")

    print(f"\n{Colors.BOLD}1{Colors.END} - Войти в систему")
    print(f"{Colors.BOLD}2{Colors.END} - Зарегистрироваться")

    if auth_token and user_info:
        print(f"{Colors.BOLD}3{Colors.END} - Выбрать упражнение")
        print(f"{Colors.BOLD}4{Colors.END} - Мой профиль")
        print(f"{Colors.BOLD}5{Colors.END} - Статистика")

    print(f"{Colors.BOLD}q{Colors.END} - Выход")
    print("=" * 60)

def print_stats_menu():
    """Вывод меню статистики"""
    clear_screen()
    print_header("📊 СТАТИСТИКА")
    print(f"{Colors.BOLD}1{Colors.END} - Общая статистика")
    print(f"{Colors.BOLD}2{Colors.END} - Статистика за сегодня")
    print(f"{Colors.BOLD}3{Colors.END} - Статистика за неделю")
    print(f"{Colors.BOLD}4{Colors.END} - Статистика за месяц")
    print(f"{Colors.BOLD}5{Colors.END} - Статистика по упражнениям")
    print(f"{Colors.BOLD}6{Colors.END} - История тренировок")
    print(f"{Colors.BOLD}b{Colors.END} - Назад в главное меню")
    print("=" * 60)

def print_exercise_menu():
    """Вывод меню упражнений"""
    clear_screen()
    print_header("🎮 ВЫБОР УПРАЖНЕНИЯ")
    for key, name in EXERCISE_NAMES.items():
        print(f"{Colors.BOLD}{key}{Colors.END} - {name}")
    print(f"{Colors.BOLD}b{Colors.END} - Назад в главное меню")
    print("=" * 60)

def login():
    """Вход в систему"""
    global auth_token, user_info

    clear_screen()
    print_header("🔐 ВХОД В СИСТЕМУ")

    email = input("Email: ").strip()
    password = input("Пароль: ").strip()

    try:
        response = requests.post(
            f"{BASE_URL}/api/login",
            json={"email": email, "password": password},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            data = response.json()
            auth_token = data["token"]
            user_info = data["user"]
            print(f"\n{Colors.GREEN}✅ Успешный вход! Добро пожаловать, {user_info['username']}!{Colors.END}")
            time.sleep(1)
            return True
        else:
            error = response.json().get("error", "Unknown error")
            print(f"\n{Colors.RED}❌ Ошибка входа: {error}{Colors.END}")
            input("\nНажмите Enter для продолжения...")
            return False

    except requests.exceptions.ConnectionError:
        print(f"\n{Colors.RED}❌ Не удалось подключиться к серверу {BASE_URL}. Убедитесь, что сервер запущен.{Colors.END}")
        input("\nНажмите Enter для продолжения...")
        return False
    except Exception as e:
        print(f"\n{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")
        return False

def register():
    """Регистрация нового пользователя"""
    clear_screen()
    print_header("📝 РЕГИСТРАЦИЯ")

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
            f"{BASE_URL}/api/register",
            json=data,
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 201:
            print(f"\n{Colors.GREEN}✅ Регистрация успешна! Теперь можете войти.{Colors.END}")
            input("\nНажмите Enter для продолжения...")
            return True
        else:
            error = response.json().get("error", "Unknown error")
            print(f"\n{Colors.RED}❌ Ошибка регистрации: {error}{Colors.END}")
            input("\nНажмите Enter для продолжения...")
            return False

    except requests.exceptions.ConnectionError:
        print(f"\n{Colors.RED}❌ Не удалось подключиться к серверу {BASE_URL}. Убедитесь, что сервер запущен.{Colors.END}")
        input("\nНажмите Enter для продолжения...")
        return False
    except Exception as e:
        print(f"\n{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")
        return False

def get_profile():
    """Получение информации о профиле"""
    global auth_token

    if not auth_token:
        print(f"{Colors.RED}❌ Не авторизован{Colors.END}")
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/profile",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            profile = response.json()
            clear_screen()
            print_header("👤 ПРОФИЛЬ ПОЛЬЗОВАТЕЛЯ")
            print(f"ID: {profile.get('id')}")
            print(f"Username: {profile.get('username')}")
            print(f"Email: {profile.get('email')}")
            print(f"Имя: {profile.get('first_name', '')}")
            print(f"Фамилия: {profile.get('last_name', '')}")
            print(f"Дата регистрации: {profile.get('created_at', '')[:10]}")
            print("=" * 40)
        else:
            print(f"{Colors.RED}❌ Ошибка получения профиля: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def start_workout():
    """Начать новую тренировку"""
    global auth_token
    headers = {"Authorization": f"Bearer {auth_token}"}
    try:
        response = requests.post(
            f"{BASE_URL}/api/workout/start",
            headers=headers,
            timeout=REQUEST_TIMEOUT
        )
        if response.status_code == 200:
            data = response.json()
            return data['id']
        else:
            print(f"{Colors.RED}❌ Ошибка начала тренировки: {response.status_code}{Colors.END}")
            return None
    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        return None

def add_exercise_set(session_id, exercise_id, repetitions, duration, accuracy):
    """Добавить выполненное упражнение"""
    global auth_token
    headers = {"Authorization": f"Bearer {auth_token}"}
    data = {
        "session_id": session_id,
        "exercise_id": exercise_id,
        "actual_repetitions": repetitions,
        "actual_duration": duration,
        "accuracy_score": accuracy
    }

    print(f"\n{Colors.YELLOW}📤 Отправка статистики на сервер{Colors.END}")

    try:
        response = requests.post(
            f"{BASE_URL}/api/workout/exercise",
            headers=headers,
            json=data,
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            print(f"{Colors.GREEN}✅ Статистика сохранена!{Colors.END}")
            return True
        else:
            print(f"{Colors.RED}❌ Ошибка сохранения: {response.status_code}{Colors.END}")
            print(f"{Colors.RED}Ответ: {response.text}{Colors.END}")
            return False
    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        return False

def end_workout(session_id):
    """Завершить тренировку"""
    global auth_token
    headers = {"Authorization": f"Bearer {auth_token}"}
    data = {"session_id": session_id}
    try:
        response = requests.post(
            f"{BASE_URL}/api/workout/end",
            headers=headers,
            json=data,
            timeout=REQUEST_TIMEOUT
        )
        if response.status_code == 200:
            return True
        else:
            return False
    except Exception as e:
        return False

def get_overall_stats():
    """Получение общей статистики"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/stats/overall",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 ОБЩАЯ СТАТИСТИКА")

            if not stats or stats.get('total_sessions') is None:
                print("Нет данных для отображения")
            else:
                print(f"Всего тренировок: {stats.get('total_sessions', 0)}")
                print(f"Всего упражнений: {stats.get('total_exercises', 0)}")
                print(f"Всего повторений: {stats.get('total_repetitions', 0)}")

                total_duration = stats.get('total_duration', 0)
                hours = total_duration // 3600
                minutes = (total_duration % 3600) // 60
                print(f"Общее время: {hours} ч {minutes} мин")

                print(f"Уникальных упражнений: {stats.get('unique_exercises', 0)}")
                print(f"Текущая серия: {stats.get('current_streak', 0)} дней")
                print(f"Максимальная серия: {stats.get('longest_streak', 0)} дней")

                last_workout = stats.get('last_workout_at')
                if last_workout:
                    if isinstance(last_workout, dict) and 'Time' in last_workout:
                        print(f"Последняя тренировка: {last_workout['Time'][:10]}")
                    elif isinstance(last_workout, str):
                        print(f"Последняя тренировка: {last_workout[:10]}")
            print("=" * 40)
        else:
            print(f"{Colors.RED}❌ Ошибка получения статистики: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def get_daily_stats():
    """Получение статистики за сегодня"""
    global auth_token

    if not auth_token:
        return

    try:
        today = datetime.now().strftime("%Y-%m-%d")
        response = requests.get(
            f"{BASE_URL}/api/stats/daily?date={today}",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header(f"📊 СТАТИСТИКА ЗА {today}")

            if not stats or stats.get('total_sessions') is None:
                print("Нет данных за сегодня")
            else:
                print(f"Тренировок: {stats.get('total_sessions', 0)}")
                print(f"Упражнений: {stats.get('total_exercises', 0)}")

                duration = stats.get('total_duration_seconds', 0)
                minutes = duration // 60
                seconds = duration % 60
                print(f"Время: {minutes} мин {seconds} сек")

                print(f"Сожжено калорий: {stats.get('calories_burned', 0):.1f}")
                print(f"День выполнен: {'✅' if stats.get('completed', False) else '❌'}")
            print("=" * 40)
        else:
            print(f"{Colors.RED}❌ Ошибка получения статистики: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def get_weekly_stats():
    """Получение статистики за неделю"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/stats/weekly",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 СТАТИСТИКА ЗА НЕДЕЛЮ")

            if not stats:
                print("Нет данных за неделю")
            else:
                total_sessions = 0
                total_duration = 0
                total_exercises = 0

                for day in stats:
                    date = day.get('stat_date', '')
                    if isinstance(date, dict) and 'Time' in date:
                        date_str = date['Time'][:10]
                    elif isinstance(date, str):
                        date_str = date[:10] if date else ''
                    else:
                        date_str = str(date)[:10] if date else ''

                    sessions = day.get('total_sessions', 0)
                    duration = day.get('total_duration_seconds', 0)
                    exercises = day.get('total_exercises', 0)
                    completed = day.get('completed', False)

                    total_sessions += sessions
                    total_duration += duration
                    total_exercises += exercises

                    status = "✅" if completed else "❌"
                    minutes = duration // 60
                    print(f"{date_str}: {status} {sessions} тр, {exercises} упр, {minutes} мин")

                print("-" * 40)
                hours = total_duration // 3600
                minutes = (total_duration % 3600) // 60
                print(f"ИТОГО: {total_sessions} тренировок, {total_exercises} упражнений")
                print(f"ВСЕГО ВРЕМЕНИ: {hours} ч {minutes} мин")
            print("=" * 60)
        else:
            print(f"{Colors.RED}❌ Ошибка получения статистики: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def get_monthly_stats():
    """Получение статистики за месяц"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/stats/monthly",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 СТАТИСТИКА ЗА МЕСЯЦ")

            if not stats:
                print("Нет данных за месяц")
            else:
                weeks = {}
                for day in stats:
                    date = day.get('stat_date', '')
                    if isinstance(date, dict) and 'Time' in date:
                        date_str = date['Time'][:10]
                    elif isinstance(date, str):
                        date_str = date[:10] if date else ''
                    else:
                        date_str = str(date)[:10] if date else ''

                    try:
                        week_num = datetime.fromisoformat(date_str).isocalendar()[1]
                        if week_num not in weeks:
                            weeks[week_num] = {
                                'sessions': 0,
                                'exercises': 0,
                                'duration': 0
                            }
                        weeks[week_num]['sessions'] += day.get('total_sessions', 0)
                        weeks[week_num]['exercises'] += day.get('total_exercises', 0)
                        weeks[week_num]['duration'] += day.get('total_duration_seconds', 0)
                    except:
                        pass

                for week_num, data in weeks.items():
                    hours = data['duration'] // 3600
                    minutes = (data['duration'] % 3600) // 60
                    print(f"Неделя {week_num}: {data['sessions']} тр, {data['exercises']} упр, {hours} ч {minutes} мин")
            print("=" * 60)
        else:
            print(f"{Colors.RED}❌ Ошибка получения статистики: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def get_exercise_stats():
    """Получение статистики по упражнениям"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/stats/exercises",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 СТАТИСТИКА ПО УПРАЖНЕНИЯМ")

            if not stats:
                print("Нет данных по упражнениям")
            else:
                for ex in stats:
                    print(f"\n🏋️ {ex.get('exercise_name', 'Неизвестно')}")
                    print(f"  Сессий: {ex.get('total_sessions', 0)}")
                    print(f"  Повторений: {ex.get('total_repetitions', 0)}")

                    duration = ex.get('total_duration', 0)
                    minutes = duration // 60
                    print(f"  Время: {minutes} мин")

                    if ex.get('best_accuracy'):
                        print(f"  Лучшая точность: {ex.get('best_accuracy'):.1f}%")
                    if ex.get('avg_accuracy'):
                        print(f"  Средняя точность: {ex.get('avg_accuracy'):.1f}%")

                    last = ex.get('last_performed_at')
                    if last:
                        if isinstance(last, dict) and 'Time' in last:
                            print(f"  Последний раз: {last['Time'][:10]}")
                        elif isinstance(last, str):
                            print(f"  Последний раз: {last[:10]}")
            print("=" * 60)
        else:
            print(f"{Colors.RED}❌ Ошибка получения статистики: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def get_workout_history():
    """Получение истории тренировок"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            f"{BASE_URL}/api/workout/history",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=REQUEST_TIMEOUT
        )

        if response.status_code == 200:
            history = response.json()
            clear_screen()
            print_header("📋 ИСТОРИЯ ТРЕНИРОВОК")

            if not history:
                print("Нет данных о тренировках")
            else:
                for i, workout in enumerate(history, 1):
                    date = workout.get('started_at', '')
                    if isinstance(date, dict) and 'Time' in date:
                        date_str = date['Time'][:10]
                    elif isinstance(date, str):
                        date_str = date[:10] if date else ''
                    else:
                        date_str = str(date)[:10] if date else ''

                    exercises = workout.get('total_exercises', 0)
                    reps = workout.get('total_reps', 0)
                    duration = workout.get('total_duration', 0)
                    accuracy = workout.get('avg_accuracy', 0)

                    minutes = duration // 60
                    print(f"\n{i}. {date_str}")
                    print(f"   Упражнений: {exercises}, Повторений: {reps}")
                    print(f"   Время: {minutes} мин, Точность: {accuracy:.1f}%")

                    for ex in workout.get('exercises', []):
                        ex_name = ex.get('name', 'Неизвестно')
                        ex_reps = ex.get('repetitions', 0)
                        print(f"   - {ex_name}: {ex_reps} раз")
            print("=" * 60)
        else:
            print(f"{Colors.RED}❌ Ошибка получения истории: {response.status_code}{Colors.END}")

        input("\nНажмите Enter для продолжения...")

    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        input("\nНажмите Enter для продолжения...")

def display_fist_palm_progress(data):
    """Отображает прогресс для упражнения Кулак-ладонь"""
    clear_screen()

    structured = data.get('structured', {})

    state = structured.get('state', 'unknown')
    current_cycle = structured.get('current_cycle', 0)
    total_cycles = structured.get('total_cycles', 5)
    countdown = structured.get('countdown')
    progress = structured.get('progress_percent', 0)
    message = data.get('message', '')

    print_header(f"🎯 {EXERCISE_NAMES['3']}")

    hand = data.get('hand_detected', False)
    hand_symbol = "🖐️" if hand else "❌"
    print(f"{hand_symbol} Рука: {'в кадре' if hand else 'не обнаружена'}")

    finger_states = data.get('finger_states', [])
    if finger_states:
        finger_names = ["Большой", "Указат", "Средний", "Безым", "Мизинец"]
        finger_status = []
        for i, s in enumerate(finger_states):
            if s:
                finger_status.append(f"{finger_names[i]}:⬆️")
            else:
                finger_status.append(f"{finger_names[i]}:⬇️")
        print(f"🖐️ Пальцы: {', '.join(finger_status)}")

    print("-" * 60)

    steps = [
        {"name": "Сожмите кулак", "state": "waiting_fist"},
        {"name": "Держите кулак", "state": "holding_fist"},
        {"name": "Раскройте ладонь", "state": "waiting_palm"},
        {"name": "Держите ладонь", "state": "holding_palm"}
    ]

    print("📋 ПРОГРЕСС УПРАЖНЕНИЯ:")

    current_step_index = -1
    for i, step in enumerate(steps):
        if step["state"] == state:
            current_step_index = i
            break

    for i, step in enumerate(steps):
        if i < current_step_index:
            print(f"  {Colors.GREEN}✅ {step['name']}{Colors.END}")
        elif i == current_step_index:
            if "holding" in str(state) and countdown is not None:
                print(f"  {Colors.YELLOW}▶️ {step['name']} [{countdown}с]{Colors.END}")
            else:
                print(f"  {Colors.YELLOW}▶️ {step['name']}{Colors.END}")
        else:
            print(f"  ⬜ {step['name']}")

    print(f"\n🔄 Цикл: {current_cycle}/{total_cycles}")

    if "holding" in str(state) and countdown is not None:
        bar_length = 30
        filled = int(progress / 100 * bar_length)
        bar = "█" * filled + "░" * (bar_length - filled)
        print(f"\n{Colors.CYAN}⏱️  Осталось: {countdown}с [{bar}] {progress:.0f}%{Colors.END}")

    if "🎉" in message:
        print(f"\n{Colors.GREEN}{message}{Colors.END}")
    elif "❌" in message:
        print(f"\n{Colors.RED}{message}{Colors.END}")
    else:
        print(f"\n{Colors.YELLOW}{message}{Colors.END}")

    print("-" * 60)

def display_regular_exercise(data, exercise_name):
    """Отображает обычное упражнение"""
    clear_screen()
    print_header(f"🎯 {exercise_name}")

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
        for i, s in enumerate(finger_states):
            if s:
                status.append(f"{Colors.GREEN}{finger_names[i]}⬆️{Colors.END}")
            else:
                status.append(f"{Colors.RED}{finger_names[i]}⬇️{Colors.END}")
        print(f"🖐️ {' | '.join(status)}")

    if "❌" in msg:
        print(f"\n{Colors.RED}{msg}{Colors.END}")
    elif "✅" in msg:
        print(f"\n{Colors.GREEN}{msg}{Colors.END}")
    else:
        print(f"\n{Colors.YELLOW}{msg}{Colors.END}")

    print("-" * 60)

def reset_exercise_on_server():
    """Отправляет запрос на сброс упражнения на сервере"""
    global auth_token
    try:
        response = requests.post(
            f"{BASE_URL}/api/exercise/reset",
            headers={"Authorization": f"Bearer {auth_token}"},
            json={"exercise_type": "fist-palm"},
            timeout=2
        )
        if response.status_code == 200:
            data = response.json()
            print(f"{Colors.GREEN}✅ Упражнение сброшено на сервере: {data.get('message')}{Colors.END}")
            return True
        else:
            print(f"{Colors.YELLOW}⚠️ Сервер вернул ошибку: {response.status_code} - {response.text}{Colors.END}")
    except Exception as e:
        print(f"{Colors.YELLOW}⚠️ Не удалось сбросить через HTTP: {e}{Colors.END}")

    return False

def connect_and_run(exercise_key):
    """Подключение и выполнение упражнения"""
    global auth_token, user_info

    if not auth_token or not user_info:
        print(f"{Colors.RED}❌ Необходимо войти в систему{Colors.END}")
        return False

    url = EXERCISE_URLS[exercise_key]
    exercise_name = EXERCISE_NAMES[exercise_key]
    exercise_type = EXERCISE_TYPES[exercise_key]

    while True:
        # Проверяем состояние упражнения
        print(f"\n{Colors.CYAN}⏳ Проверка состояния упражнения...{Colors.END}")

        if not wait_for_exercise_reset(exercise_type):
            print(f"{Colors.YELLOW}⚠️ Принудительный сброс через API...{Colors.END}")
            try:
                reset_response = requests.post(
                    f"{BASE_URL}/api/exercise/reset",
                    headers={"Authorization": f"Bearer {auth_token}"},
                    json={"exercise_type": exercise_type},
                    timeout=2
                )
                if reset_response.status_code == 200:
                    print(f"{Colors.GREEN}✅ Упражнение принудительно сброшено{Colors.END}")
                time.sleep(1)
            except:
                pass

        print(f"\n{Colors.CYAN}🔄 Начинаем тренировку...{Colors.END}")
        session_id = start_workout()
        if not session_id:
            return False

        encoded_token = urllib.parse.quote(auth_token)
        ws_url = f"{url}?token={encoded_token}"

        print(f"{Colors.GREEN}✅ Тренировка начата, ID: {session_id}{Colors.END}")
        print(f"\n🔌 Подключение к WebSocket...")
        print(f"📋 Упражнение: {exercise_name}")
        print(f"👤 Пользователь: {user_info.get('username')}")

        try:
            ws = websocket.create_connection(ws_url, timeout=CONNECTION_TIMEOUT)
            print(f"{Colors.GREEN}✅ WebSocket подключен успешно!{Colors.END}")

            camera = cv2.VideoCapture(0)
            camera.set(cv2.CAP_PROP_FRAME_WIDTH, FRAME_WIDTH)
            camera.set(cv2.CAP_PROP_FRAME_HEIGHT, FRAME_HEIGHT)

            if not camera.isOpened():
                print(f"{Colors.RED}❌ Не удалось открыть камеру{Colors.END}")
                return False

            print(f"\n{Colors.CYAN}📹 Отправка кадров... Нажмите ESC для выхода{Colors.END}")
            print("-" * 60)

            frame_count = 0
            last_update_time = time.time()
            sets_completed = 0
            last_cycle = -1
            exercise_completed = False
            stats_saved_for_cycle = set()
            workout_ended = False
            total_cycles = 5

            while True:
                good, img = camera.read()
                if not good:
                    continue

                frame_count += 1

                if frame_count % FRAME_SKIP == 0:
                    _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, JPEG_QUALITY])
                    img_base64 = base64.b64encode(buffer).decode('utf-8')

                    ws.send(json.dumps({
                        "frame": img_base64,
                        "exercise_type": exercise_type
                    }))

                    ws.settimeout(WS_TIMEOUT)
                    try:
                        result = ws.recv()
                        data = json.loads(result)

                        if 'processed_frame' in data and data['processed_frame']:
                            frame_bytes = base64.b64decode(data['processed_frame'])
                            nparr = np.frombuffer(frame_bytes, np.uint8)
                            processed = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                            if processed is not None:
                                cv2.imshow('Processed', processed)

                        current_time = time.time()
                        if current_time - last_update_time > 0.5:
                            if exercise_key == '3':
                                display_fist_palm_progress(data)

                                structured = data.get('structured', {})
                                current_cycle = structured.get('current_cycle', 0)
                                total_cycles = structured.get('total_cycles', 5)
                                completed = structured.get('completed', False)

                                if current_cycle > last_cycle and last_cycle >= 0:
                                    completed_cycle = last_cycle
                                    if completed_cycle not in stats_saved_for_cycle and completed_cycle > 0:
                                        stats_saved_for_cycle.add(completed_cycle)
                                        sets_completed = len(stats_saved_for_cycle)
                                        print(f"\n{Colors.GREEN}✅ ЦИКЛ {completed_cycle}/{total_cycles} ЗАВЕРШЕН!{Colors.END}")

                                        if add_exercise_set(session_id, exercise_type, 5, 60, 95.0):
                                            print(f"{Colors.GREEN}✅ Статистика сохранена!{Colors.END}")
                                        else:
                                            print(f"{Colors.RED}❌ Ошибка сохранения статистики{Colors.END}")

                                if completed and not exercise_completed:
                                    if total_cycles not in stats_saved_for_cycle:
                                        stats_saved_for_cycle.add(total_cycles)
                                        sets_completed = len(stats_saved_for_cycle)
                                        print(f"\n{Colors.GREEN}✅ ЦИКЛ {total_cycles}/{total_cycles} ЗАВЕРШЕН!{Colors.END}")

                                        if add_exercise_set(session_id, exercise_type, 5, 60, 95.0):
                                            print(f"{Colors.GREEN}✅ Статистика сохранена!{Colors.END}")

                                    exercise_completed = True
                                    print(f"\n{Colors.YELLOW}🎯 УПРАЖНЕНИЕ ВЫПОЛНЕНО!{Colors.END}")

                                    if end_workout(session_id):
                                        print(f"{Colors.GREEN}✅ Тренировка завершена!{Colors.END}")

                                    workout_ended = True
                                    break

                                last_cycle = current_cycle
                            else:
                                display_regular_exercise(data, exercise_name)

                            last_update_time = current_time

                    except websocket.WebSocketTimeoutException:
                        pass
                    except Exception as e:
                        print(f"\n{Colors.RED}❌ Ошибка получения: {e}{Colors.END}")

                cv2.putText(img, f"User: {user_info.get('username', '')}", (10, 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
                cv2.putText(img, f"Sets: {sets_completed}/{total_cycles}", (10, 55),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
                cv2.putText(img, "ESC - exit", (10, 80),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

                cv2.imshow('Original', img)

                key = cv2.waitKey(1) & 0xFF
                if key == 27:
                    workout_ended = True
                    break

                if exercise_completed:
                    break

            camera.release()
            cv2.destroyAllWindows()

            try:
                ws.close()
                print("🔌 WebSocket соединение закрыто")
            except:
                pass

            if sets_completed > 0:
                print(f"\n{Colors.GREEN}📊 ИТОГО ВЫПОЛНЕНО: {sets_completed}/{total_cycles} подходов{Colors.END}")

            print("\n🔌 Соединение закрыто")

            if exercise_completed:
                print(f"\n{Colors.CYAN}Хотите выполнить это упражнение еще раз? (y/n): {Colors.END}", end="")
                choice = input().strip().lower()

                if choice in ['y', 'д', 'yes', 'да']:
                    print(f"{Colors.GREEN}🔄 Начинаем новое выполнение...{Colors.END}")
                    time.sleep(2)
                    continue
                else:
                    print(f"{Colors.BLUE}⏹️ Возврат в меню упражнений...{Colors.END}")
                    time.sleep(1)
                    break
            else:
                break

        except websocket.WebSocketBadStatusException as e:
            if "401" in str(e):
                print(f"\n{Colors.RED}❌ Ошибка авторизации. Токен недействителен.{Colors.END}")
                auth_token = None
                user_info = None
            else:
                print(f"\n{Colors.RED}❌ Ошибка WebSocket: {e}{Colors.END}")
            input("\nНажмите Enter для продолжения...")
            return False
        except Exception as e:
            print(f"\n{Colors.RED}❌ Ошибка: {e}{Colors.END}")
            import traceback
            traceback.print_exc()
            input("\nНажмите Enter для продолжения...")
            return False

    return True

def check_exercise_state(exercise_type):
    """Проверяет состояние упражнения на сервере"""
    global auth_token
    try:
        response = requests.get(
            f"{BASE_URL}/api/exercise_state?type={exercise_type}",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=3
        )
        if response.status_code == 200:
            data = response.json()
            if data.get('structured'):
                print(f"📊 Получено состояние: {data['structured']}")
            return data
        else:
            print(f"{Colors.YELLOW}⚠️ Не удалось получить состояние: {response.status_code}{Colors.END}")
            return None
    except Exception as e:
        print(f"{Colors.YELLOW}⚠️ Ошибка при проверке состояния: {e}{Colors.END}")
        return None

def wait_for_exercise_reset(exercise_type, max_attempts=10):
    """Ждет пока упражнение не сбросится или не будет готово к началу"""
    for attempt in range(max_attempts):
        state = check_exercise_state(exercise_type)
        if state and state.get('structured'):
            structured = state.get('structured', {})
            current_cycle = structured.get('current_cycle', 0)
            state_name = structured.get('state', '')
            completed = structured.get('completed', False)
            auto_reset = structured.get('auto_reset', False)

            print(f"🔄 Проверка состояния: цикл={current_cycle}, состояние={state_name}, завершено={completed}")

            if not completed and current_cycle == 0 and state_name == 'waiting_fist':
                print(f"{Colors.GREEN}✅ Упражнение готово к началу{Colors.END}")
                return True

            if completed and auto_reset:
                print(f"{Colors.YELLOW}⏳ Упражнение завершено, но будет сброшено при подключении...{Colors.END}")
                time.sleep(2)
                continue

        time.sleep(1)

    print(f"{Colors.YELLOW}⚠️ Упражнение все еще в состоянии completed, пробуем принудительный сброс{Colors.END}")
    return False

def main():
    global auth_token, user_info

    print("=" * 60)
    print("🎮 ТЕСТОВЫЙ КЛИЕНТ ДЛЯ LFK")
    print("=" * 60)
    print(f"🌐 Сервер: {BASE_URL}")
    print("=" * 60)

    while True:
        print_menu()
        choice = input("\nВыберите действие: ").strip().lower()

        if choice == 'q':
            print(f"\n{Colors.BLUE}👋 До свидания!{Colors.END}")
            break

        elif choice == '1':
            login()

        elif choice == '2':
            register()

        elif choice == '3' and auth_token and user_info:
            while True:
                print_exercise_menu()
                ex_choice = input("\nВыберите упражнение (1-3, b - назад): ").strip().lower()

                if ex_choice == 'b':
                    break

                if ex_choice in EXERCISE_URLS:
                    connect_and_run(ex_choice)
                else:
                    print(f"{Colors.RED}❌ Неверный выбор{Colors.END}")
                    time.sleep(1)

        elif choice == '4' and auth_token and user_info:
            get_profile()

        elif choice == '5' and auth_token and user_info:
            while True:
                print_stats_menu()
                stat_choice = input("\nВыберите пункт статистики: ").strip().lower()

                if stat_choice == 'b':
                    break
                elif stat_choice == '1':
                    get_overall_stats()
                elif stat_choice == '2':
                    get_daily_stats()
                elif stat_choice == '3':
                    get_weekly_stats()
                elif stat_choice == '4':
                    get_monthly_stats()
                elif stat_choice == '5':
                    get_exercise_stats()
                elif stat_choice == '6':
                    get_workout_history()
                else:
                    print(f"{Colors.RED}❌ Неверный выбор{Colors.END}")
                    time.sleep(1)

        elif choice in ['3', '4', '5'] and (not auth_token or not user_info):
            print(f"{Colors.RED}❌ Сначала войдите в систему{Colors.END}")
            time.sleep(1)

        else:
            print(f"{Colors.RED}❌ Неверный выбор{Colors.END}")
            time.sleep(1)

if __name__ == "__main__":
    main()