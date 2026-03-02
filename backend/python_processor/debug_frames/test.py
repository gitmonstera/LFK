import requests
import json
from pprint import pprint

# Конфигурация
BASE_URL = "http://localhost:8080"
TEST_USER = {
    "email": "test@test.com",
    "password": "password"
}

# Цвета для красивого вывода
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    CYAN = '\033[96m'
    END = '\033[0m'
    BOLD = '\033[1m'

def print_header(text):
    """Печать заголовка"""
    print(f"\n{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{text}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")

def print_success(text):
    """Печать успешного сообщения"""
    print(f"{Colors.GREEN}✅ {text}{Colors.END}")

def print_error(text):
    """Печать сообщения об ошибке"""
    print(f"{Colors.RED}❌ {text}{Colors.END}")

def print_info(text):
    """Печать информационного сообщения"""
    print(f"{Colors.CYAN}ℹ️ {text}{Colors.END}")

def print_json(data):
    """Печать JSON с форматированием"""
    print(json.dumps(data, indent=2, ensure_ascii=False))

def login():
    """Авторизация и получение токена"""
    print_info("Авторизация...")

    try:
        response = requests.post(
            f"{BASE_URL}/api/login",
            json=TEST_USER,
            timeout=5
        )

        if response.status_code == 200:
            data = response.json()
            token = data.get("token")
            user = data.get("user")
            print_success(f"Успешный вход! Пользователь: {user.get('username')}")
            return token
        else:
            print_error(f"Ошибка авторизации: {response.status_code}")
            print_json(response.json())
            return None

    except Exception as e:
        print_error(f"Ошибка подключения: {e}")
        return None

def test_get_exercise_list(token):
    """Тест получения списка упражнений"""
    print_header("ТЕСТ 1: Получение списка упражнений")

    try:
        response = requests.get(
            f"{BASE_URL}/api/get_exercise_list",
            headers={"Authorization": f"Bearer {token}"},
            timeout=5
        )

        print_info(f"Статус ответа: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            exercises = data.get('items', [])

            print_success(f"Получено {len(exercises)} упражнений")
            print("\n" + "="*60)

            # Выводим информацию о каждом упражнении
            for i, ex in enumerate(exercises, 1):
                print(f"\n{Colors.BOLD}{i}. {ex.get('name')}{Colors.END}")
                print(f"   ID: {Colors.CYAN}{ex.get('exercise_id')}{Colors.END}")
                print(f"   Описание: {ex.get('description')}")
                print(f"   Категория: {ex.get('category')} {ex.get('category_icon', '')}")
                print(f"   Цвет категории: {ex.get('category_color', '')}")
                print(f"   Сложность: {ex.get('difficulty_level')}/5")
                print(f"   Мышцы: {', '.join(ex.get('target_muscles', []))}")
                print(f"   Инструкции:")
                for j, instr in enumerate(ex.get('instructions', []), 1):
                    print(f"     {j}. {instr}")
                print(f"   Длительность: {ex.get('duration_seconds')} сек")
                print(f"   Изображение: {ex.get('image_url', 'Нет')}")
                print(f"   Видео: {ex.get('video_url', 'Нет')}")
                print("-" * 40)

            return exercises
        else:
            print_error(f"Ошибка: {response.status_code}")
            print_json(response.json())
            return None

    except Exception as e:
        print_error(f"Ошибка подключения: {e}")
        return None

def test_without_auth():
    """Тест доступа без авторизации"""
    print_header("ТЕСТ 2: Доступ без токена")

    try:
        response = requests.get(
            f"{BASE_URL}/api/get_exercise_list",
            timeout=5
        )

        print_info(f"Статус ответа: {response.status_code}")

        if response.status_code == 401:
            print_success("Доступ запрещен (как и должно быть)")
            print_json(response.json())
        else:
            print_error(f"Неожиданный статус: {response.status_code}")

    except Exception as e:
        print_error(f"Ошибка подключения: {e}")

def test_invalid_token():
    """Тест с неверным токеном"""
    print_header("ТЕСТ 3: Неверный токен")

    try:
        response = requests.get(
            f"{BASE_URL}/api/get_exercise_list",
            headers={"Authorization": "Bearer invalid_token_123"},
            timeout=5
        )

        print_info(f"Статус ответа: {response.status_code}")

        if response.status_code == 401:
            print_success("Доступ запрещен (как и должно быть)")
            print_json(response.json())
        else:
            print_error(f"Неожиданный статус: {response.status_code}")

    except Exception as e:
        print_error(f"Ошибка подключения: {e}")

def test_exercise_count(exercises):
    """Проверка количества упражнений"""
    print_header("ТЕСТ 4: Проверка количества упражнений")

    expected_count = 3  # Ожидаемое количество упражнений
    actual_count = len(exercises) if exercises else 0

    print_info(f"Ожидаемое количество: {expected_count}")
    print_info(f"Фактическое количество: {actual_count}")

    if actual_count >= expected_count:
        print_success("Количество упражнений соответствует ожидаемому")
    else:
        print_error(f"Ожидалось минимум {expected_count}, получено {actual_count}")

def test_required_fields(exercises):
    """Проверка наличия обязательных полей"""
    print_header("ТЕСТ 5: Проверка обязательных полей")

    required_fields = [
        'exercise_id', 'name', 'description', 'category',
        'difficulty_level', 'target_muscles', 'instructions',
        'duration_seconds'
    ]

    all_valid = True

    for i, ex in enumerate(exercises, 1):
        print_info(f"Проверка упражнения {i}: {ex.get('name')}")
        missing_fields = []

        for field in required_fields:
            if field not in ex:
                missing_fields.append(field)
            elif field in ['target_muscles', 'instructions'] and not isinstance(ex[field], list):
                missing_fields.append(f"{field} (не массив)")

        if missing_fields:
            print_error(f"  Отсутствуют поля: {', '.join(missing_fields)}")
            all_valid = False
        else:
            print_success(f"  Все обязательные поля присутствуют")

    if all_valid:
        print_success("Все упражнения содержат обязательные поля")
    else:
        print_error("Некоторые упражнения не содержат обязательных полей")

def test_data_types(exercises):
    """Проверка типов данных"""
    print_header("ТЕСТ 6: Проверка типов данных")

    all_valid = True

    for i, ex in enumerate(exercises, 1):
        print_info(f"Проверка типов в упражнении {i}: {ex.get('name')}")

        # Проверка difficulty_level
        if not isinstance(ex.get('difficulty_level'), int):
            print_error(f"  difficulty_level должен быть числом")
            all_valid = False

        # Проверка duration_seconds
        if not isinstance(ex.get('duration_seconds'), int):
            print_error(f"  duration_seconds должен быть числом")
            all_valid = False

        # Проверка target_muscles
        if not isinstance(ex.get('target_muscles'), list):
            print_error(f"  target_muscles должен быть массивом")
            all_valid = False

        # Проверка instructions
        if not isinstance(ex.get('instructions'), list):
            print_error(f"  instructions должен быть массивом")
            all_valid = False

        # Проверка category_id (если есть)
        if 'category_id' in ex and not isinstance(ex.get('category_id'), int):
            print_error(f"  category_id должен быть числом")
            all_valid = False

    if all_valid:
        print_success("Все типы данных корректны")
    else:
        print_error("Обнаружены ошибки в типах данных")

def test_response_structure(data):
    """Проверка структуры ответа"""
    print_header("ТЕСТ 7: Проверка структуры ответа")

    if 'items' in data:
        print_success("Поле 'items' присутствует")
        if isinstance(data['items'], list):
            print_success("Поле 'items' является массивом")
        else:
            print_error("Поле 'items' должно быть массивом")
    else:
        print_error("Отсутствует поле 'items' в ответе")

def main():
    """Главная функция"""
    print_header("ТЕСТИРОВАНИЕ API /api/get_exercise_list")
    print(f"Базовый URL: {BASE_URL}")
    print(f"Тестовый пользователь: {TEST_USER['email']}")

    # Тест 1: Авторизация
    token = login()

    if not token:
        print_error("Не удалось получить токен. Завершение тестов.")
        return

    # Тест 2: Получение списка упражнений
    exercises = test_get_exercise_list(token)

    if not exercises:
        print_error("Не удалось получить список упражнений. Завершение тестов.")
        return

    # Тест 3: Доступ без авторизации
    test_without_auth()

    # Тест 4: Неверный токен
    test_invalid_token()

    # Тест 5: Проверка количества
    test_exercise_count(exercises)

    # Тест 6: Проверка обязательных полей
    test_required_fields(exercises)

    # Тест 7: Проверка типов данных
    test_data_types(exercises)

    # Тест 8: Проверка структуры
    if exercises:
        test_response_structure({'items': exercises})

    print_header("ТЕСТИРОВАНИЕ ЗАВЕРШЕНО")

if __name__ == "__main__":
    main()