import requests
import json
import time

# Базовый URL API
BASE_URL = "http://localhost:8080/api"

# Данные для входа
login_data = {
    "email": "test@test.com",
    "password": "password"
}

def login():
    """Вход в систему и получение токена"""
    response = requests.post(f"{BASE_URL}/login", json=login_data)
    if response.status_code == 200:
        data = response.json()
        print("✅ Успешный вход!")
        return data["token"]
    else:
        print(f"❌ Ошибка входа: {response.status_code}")
        print(response.text)
        return None

def start_workout(token):
    """Начать тренировку"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.post(f"{BASE_URL}/workout/start", headers=headers)
    if response.status_code == 200:
        data = response.json()
        print(f"✅ Тренировка начата: {data['id']}")
        return data["id"]
    else:
        print(f"❌ Ошибка начала тренировки: {response.status_code}")
        return None

def add_exercise_set(token, session_id, exercise_id, reps, duration, accuracy):
    """Добавить выполнение упражнения"""
    headers = {"Authorization": f"Bearer {token}"}
    data = {
        "session_id": session_id,
        "exercise_id": exercise_id,
        "actual_repetitions": reps,
        "actual_duration": duration,
        "accuracy_score": accuracy
    }
    response = requests.post(f"{BASE_URL}/workout/exercise", headers=headers, json=data)
    if response.status_code == 200:
        print(f"✅ Упражнение {exercise_id} добавлено: {reps} раз, {duration} сек, точность {accuracy}%")
        return True
    else:
        print(f"❌ Ошибка добавления упражнения: {response.status_code}")
        print(response.text)
        return False

def end_workout(token, session_id):
    """Завершить тренировку"""
    headers = {"Authorization": f"Bearer {token}"}
    data = {"session_id": session_id}
    response = requests.post(f"{BASE_URL}/workout/end", headers=headers, json=data)
    if response.status_code == 200:
        print("✅ Тренировка завершена!")
        return True
    else:
        print(f"❌ Ошибка завершения тренировки: {response.status_code}")
        return False

def get_stats(token):
    """Получить статистику"""
    headers = {"Authorization": f"Bearer {token}"}

    # Общая статистика
    response = requests.get(f"{BASE_URL}/stats/overall", headers=headers)
    if response.status_code == 200:
        print("\n📊 ОБЩАЯ СТАТИСТИКА:")
        print(json.dumps(response.json(), indent=2, ensure_ascii=False))
    else:
        print(f"❌ Ошибка получения статистики: {response.status_code}")

    # Статистика по упражнениям
    response = requests.get(f"{BASE_URL}/stats/exercises", headers=headers)
    if response.status_code == 200:
        print("\n📊 СТАТИСТИКА ПО УПРАЖНЕНИЯМ:")
        print(json.dumps(response.json(), indent=2, ensure_ascii=False))
    else:
        print(f"❌ Ошибка получения статистики: {response.status_code}")

def main():
    print("🚀 ТЕСТОВЫЙ СКРИПТ ДЛЯ ЗАПОЛНЕНИЯ СТАТИСТИКИ")
    print("=" * 50)

    # Вход
    token = login()
    if not token:
        return

    # Создаем тестовые данные для разных упражнений
    exercises = [
        {"id": "fist", "name": "Кулак", "reps": 10, "duration": 30, "accuracy": 95.5},
        {"id": "fist-index", "name": "Кулак с указательным", "reps": 8, "duration": 25, "accuracy": 88.0},
        {"id": "fist-palm", "name": "Кулак-ладонь", "reps": 5, "duration": 60, "accuracy": 92.3},
    ]

    # Выполняем несколько тренировок
    for workout_num in range(1, 4):
        print(f"\n🏋️ ТРЕНИРОВКА #{workout_num}")
        print("-" * 30)

        # Начинаем тренировку
        session_id = start_workout(token)
        if not session_id:
            continue

        # Добавляем упражнения
        for ex in exercises:
            time.sleep(0.5)  # Небольшая пауза между упражнениями
            add_exercise_set(token, session_id, ex["id"],
                             ex["reps"] * workout_num,
                             ex["duration"] * workout_num,
                             ex["accuracy"] - workout_num)

        # Завершаем тренировку
        end_workout(token, session_id)

    # Получаем статистику
    get_stats(token)

if __name__ == "__main__":
    main()