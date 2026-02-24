import requests
import json

# Данные для входа
login_data = {
    "email": "e@e.com",
    "password": "password"
}

print("="*60)
print("🔧 ТЕСТ СОХРАНЕНИЯ СТАТИСТИКИ")
print("="*60)

# 1. Логинимся
print("\n1️⃣  Вход в систему...")
response = requests.post("http://localhost:8080/api/login", json=login_data)
if response.status_code != 200:
    print(f"❌ Ошибка входа: {response.status_code}")
    exit()

token = response.json()["token"]
print(f"✅ Успешный вход! Токен получен")

# 2. Начинаем тренировку
print("\n2️⃣  Начало тренировки...")
headers = {"Authorization": f"Bearer {token}"}
response = requests.post("http://localhost:8080/api/workout/start", headers=headers)
if response.status_code != 200:
    print(f"❌ Ошибка начала тренировки: {response.status_code}")
    exit()

session_id = response.json()["id"]
print(f"✅ Тренировка начата, ID: {session_id}")

# 3. Отправляем тестовое упражнение
print("\n3️⃣  Отправка тестового упражнения...")
data = {
    "session_id": session_id,
    "exercise_id": "fist-palm",
    "actual_repetitions": 5,
    "actual_duration": 60,
    "accuracy_score": 95.0
}

print(f"📤 Отправляем данные:")
print(json.dumps(data, indent=2))

response = requests.post(
    "http://localhost:8080/api/workout/exercise",
    headers=headers,
    json=data
)

if response.status_code == 200:
    print(f"✅ Упражнение успешно добавлено!")
    print(f"📥 Ответ: {response.json()}")
else:
    print(f"❌ Ошибка: {response.status_code}")
    print(f"📥 Ответ: {response.text}")

# 4. Завершаем тренировку
print("\n4️⃣  Завершение тренировки...")
response = requests.post(
    "http://localhost:8080/api/workout/end",
    headers=headers,
    json={"session_id": session_id}
)

if response.status_code == 200:
    print(f"✅ Тренировка завершена!")
else:
    print(f"❌ Ошибка завершения: {response.status_code}")

print("\n5️⃣  Проверка статистики...")
response = requests.get(
    "http://localhost:8080/api/stats/overall",
    headers=headers
)

if response.status_code == 200:
    stats = response.json()
    print(f"📊 Общая статистика:")
    print(json.dumps(stats, indent=2))
else:
    print(f"❌ Ошибка получения статистики: {response.status_code}")

print("\n" + "="*60)