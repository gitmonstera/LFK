import cv2
import websocket
import base64
import json
import numpy as np
import time
import requests
import urllib.parse
from datetime import datetime, timedelta

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

# Цвета для красивого вывода
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
            "http://localhost:8080/api/login",
            json={"email": email, "password": password},
            timeout=5
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
        print(f"\n{Colors.RED}❌ Не удалось подключиться к серверу. Убедитесь, что сервер запущен.{Colors.END}")
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
            "http://localhost:8080/api/register",
            json=data,
            timeout=5
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
        print(f"\n{Colors.RED}❌ Не удалось подключиться к серверу. Убедитесь, что сервер запущен.{Colors.END}")
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
            "http://localhost:8080/api/profile",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
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

# ============= ФУНКЦИИ ДЛЯ РАБОТЫ С ТРЕНИРОВКАМИ =============

def start_workout():
    """Начать новую тренировку"""
    global auth_token
    headers = {"Authorization": f"Bearer {auth_token}"}
    try:
        response = requests.post(
            "http://localhost:8080/api/workout/start",
            headers=headers,
            timeout=5
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
    try:
        response = requests.post(
            "http://localhost:8080/api/workout/exercise",
            headers=headers,
            json=data,
            timeout=5
        )
        return response.status_code == 200
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
            "http://localhost:8080/api/workout/end",
            headers=headers,
            json=data,
            timeout=5
        )
        return response.status_code == 200
    except Exception as e:
        print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
        return False

# ============= ФУНКЦИИ ДЛЯ СТАТИСТИКИ =============

def get_overall_stats():
    """Получение общей статистики"""
    global auth_token

    if not auth_token:
        return

    try:
        response = requests.get(
            "http://localhost:8080/api/stats/overall",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 ОБЩАЯ СТАТИСТИКА")

            if 'message' in stats and stats['message'] == 'No stats available':
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
                    if isinstance(last_workout, dict):
                        if last_workout.get('Valid'):
                            date_str = last_workout.get('Time', '')
                            if date_str:
                                print(f"Последняя тренировка: {date_str[:10]}")
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
            f"http://localhost:8080/api/stats/daily?date={today}",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header(f"📊 СТАТИСТИКА ЗА {today}")

            if 'message' in stats and stats['message'] == 'No data for this date':
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
            "http://localhost:8080/api/stats/weekly",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
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
                    if isinstance(date, dict):
                        date_str = date.get('Time', '')[:10] if date.get('Time') else ''
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
            "http://localhost:8080/api/stats/monthly",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
        )

        if response.status_code == 200:
            stats = response.json()
            clear_screen()
            print_header("📊 СТАТИСТИКА ЗА МЕСЯЦ")

            if not stats:
                print("Нет данных за месяц")
            else:
                # Группировка по неделям
                weeks = {}
                for day in stats:
                    date = day.get('stat_date', '')
                    if isinstance(date, dict):
                        date_str = date.get('Time', '')[:10] if date.get('Time') else ''
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
            "http://localhost:8080/api/stats/exercises",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
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
                        if isinstance(last, dict):
                            if last.get('Valid'):
                                date_str = last.get('Time', '')
                                if date_str:
                                    print(f"  Последний раз: {date_str[:10]}")
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
            "http://localhost:8080/api/workout/history",
            headers={"Authorization": f"Bearer {auth_token}"},
            timeout=5
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
                    if isinstance(date, dict):
                        date_str = date.get('Time', '')[:10] if date.get('Time') else ''
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

                    # Показываем детали упражнений
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

# ============= ФУНКЦИИ ОТОБРАЖЕНИЯ УПРАЖНЕНИЙ =============

def display_fist_palm_progress(data):
    """Отображает прогресс для упражнения Кулак-ладонь"""
    clear_screen()

    structured = data.get('structured', {})

    # Получаем данные с безопасной обработкой
    state = structured.get('state', 'unknown')
    if state is None:
        state = 'unknown'

    current_cycle = structured.get('current_cycle', 0)
    if current_cycle is None:
        current_cycle = 0

    total_cycles = structured.get('total_cycles', 5)
    if total_cycles is None:
        total_cycles = 5

    countdown = structured.get('countdown')
    progress = structured.get('progress_percent', 0)
    if progress is None:
        progress = 0

    message = data.get('message', '')

    print_header(f"🎯 {EXERCISE_NAMES['3']}")

    # Статус руки
    hand = data.get('hand_detected', False)
    hand_symbol = "🖐️" if hand else "❌"
    print(f"{hand_symbol} Рука: {'в кадре' if hand else 'не обнаружена'}")

    # Состояние пальцев
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

    # Отображение шагов
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
            print(f"  {Colors.GREEN}✅ {step['name']}{Colors.END}")
        elif i == current_step_index:
            if "holding" in str(state) and countdown is not None:
                print(f"  {Colors.YELLOW}▶️ {step['name']} [{countdown}с]{Colors.END}")
            else:
                print(f"  {Colors.YELLOW}▶️ {step['name']}{Colors.END}")
        else:
            print(f"  ⬜ {step['name']}")

    print(f"\n🔄 Цикл: {current_cycle}/{total_cycles}")

    # Прогресс-бар
    if "holding" in str(state) and countdown is not None:
        bar_length = 30
        filled = int(progress / 100 * bar_length)
        bar = "█" * filled + "░" * (bar_length - filled)
        print(f"\n{Colors.CYAN}⏱️  Осталось: {countdown}с [{bar}] {progress:.0f}%{Colors.END}")

    # Сообщение
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

def connect_and_run(exercise_key):
    """Подключение и выполнение упражнения"""
    global auth_token, user_info

    if not auth_token or not user_info:
        print(f"{Colors.RED}❌ Необходимо войти в систему{Colors.END}")
        return False

    url = EXERCISE_URLS[exercise_key]
    exercise_name = EXERCISE_NAMES[exercise_key]
    exercise_type = EXERCISE_TYPES[exercise_key]

    # Начинаем тренировку
    print(f"\n{Colors.CYAN}🔄 Начинаем тренировку...{Colors.END}")
    session_id = start_workout()
    if not session_id:
        return False

    # Кодируем токен для URL
    encoded_token = urllib.parse.quote(auth_token)
    ws_url = f"{url}?token={encoded_token}"

    print(f"{Colors.GREEN}✅ Тренировка начата, ID: {session_id}{Colors.END}")
    print(f"\n🔌 Подключение к WebSocket...")
    print(f"📋 Упражнение: {exercise_name}")
    print(f"👤 Пользователь: {user_info.get('username')}")

    try:
        ws = websocket.create_connection(ws_url, timeout=10)
        print(f"{Colors.GREEN}✅ WebSocket подключен успешно!{Colors.END}")

        camera = cv2.VideoCapture(0)
        camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        if not camera.isOpened():
            print(f"{Colors.RED}❌ Не удалось открыть камеру{Colors.END}")
            return False

        print(f"\n{Colors.CYAN}📹 Отправка кадров... Нажмите ESC для выхода{Colors.END}")
        print("-" * 60)

        frame_count = 0
        fps_time = time.time()
        last_update_time = time.time()
        sets_completed = 0
        exercise_completed = False

        while True:
            good, img = camera.read()
            if not good:
                continue

            frame_count += 1

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

                    if 'processed_frame' in data and data['processed_frame']:
                        frame_bytes = base64.b64decode(data['processed_frame'])
                        nparr = np.frombuffer(frame_bytes, np.uint8)
                        processed = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                        if processed is not None:
                            cv2.imshow('Processed', processed)

                    # Обновляем отображение
                    current_time = time.time()
                    if current_time - last_update_time > 0.5:
                        if exercise_key == '3':
                            display_fist_palm_progress(data)

                            # Проверяем завершение упражнения
                            message = data.get('message', '')
                            if message.startswith('🎉') and not exercise_completed:
                                exercise_completed = True
                                sets_completed += 1
                                print(f"\n{Colors.GREEN}✅ ЦИКЛ {sets_completed} ЗАВЕРШЕН!{Colors.END}")

                                # Сохраняем статистику
                                if add_exercise_set(session_id, exercise_type, 5, 60, 95.0):
                                    print(f"{Colors.GREEN}✅ Статистика сохранена!{Colors.END}")

                                exercise_completed = False
                        else:
                            display_regular_exercise(data, exercise_name)

                        last_update_time = current_time

                except websocket.WebSocketTimeoutException:
                    pass
                except Exception as e:
                    print(f"\n{Colors.RED}❌ Ошибка получения: {e}{Colors.END}")

            # Добавляем информацию на кадр
            cv2.putText(img, f"User: {user_info.get('username', '')}", (10, 30),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(img, f"Sets: {sets_completed}", (10, 55),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 0), 2)
            cv2.putText(img, "ESC - exit", (10, 80),
                        cv2.FONT_HERSHEY_SIMPLEX, 0.5, (255, 255, 255), 1)

            cv2.imshow('Original', img)

            key = cv2.waitKey(1) & 0xFF
            if key == 27:  # ESC
                break

        camera.release()
        cv2.destroyAllWindows()
        ws.close()

        if sets_completed > 0:
            print(f"\n{Colors.GREEN}📊 ИТОГО ВЫПОЛНЕНО: {sets_completed} подходов{Colors.END}")

        # Завершаем тренировку
        if end_workout(session_id):
            print(f"{Colors.GREEN}✅ Тренировка завершена!{Colors.END}")

        print("\n🔌 Соединение закрыто")
        input("\nНажмите Enter для продолжения...")
        return True

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

def main():
    global auth_token, user_info

    print("=" * 60)
    print("🎮 ТЕСТОВЫЙ КЛИЕНТ ДЛЯ LFK")
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