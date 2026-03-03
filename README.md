# Лечебная Физическая Культура (ЛФК) 🏥

<div align="center">
  <img src="images/center_logo.svg" alt="LFK Logo" width="200"/>
</div>

<div align="center">
  
  ![Version](https://img.shields.io/badge/version-1.0.0-blue?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
  [![Go Report Card](https://goreportcard.com/badge/github.com/yourusername/lfk?style=flat-square)](https://goreportcard.com/report/github.com/yourusername/lfk)
  
  *Умный помощник для выполнения упражнений лечебной физкультуры*
  
  [О проекте](#-о-проекте) •
  [Технологии](#-технологии) •
  [Быстрый старт](#-быстрый-старт) •
  [Структура](#-структура-проекта) •
  [API](#-api-endpoints) •
  [Разработчик](#-разработчик)
  
</div>

---

## 📋 О проекте

**LFK** — это интеллектуальная система, которая помогает людям правильно выполнять упражнения лечебной физкультуры (ЛФК). Проект использует компьютерное зрение для анализа движений в реальном времени, предоставляет обратную связь и отслеживает прогресс пользователя.

### ✨ Возможности
- 📹 **Анализ движений** в реальном времени через камеру
- 🗣️ **Подсказки** при неправильном выполнении
- 📊 **Отслеживание прогресса** и статистика тренировок
- 📱 **Мобильное приложение** для Android
- 🔄 **WebSocket соединение** для мгновенной обратной связи
- 🗄️ **Хранение истории** тренировок в PostgreSQL

---

## 📱 Интерфейс приложения

потом будет пример интерфейса 

---

## 🛠 Технологии

<div align="center">
  
### Backend
![Go](https://img.shields.io/badge/Go-00ADD8?style=for-the-badge&logo=go&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white)

### Computer Vision
![MediaPipe](https://img.shields.io/badge/MediaPipe-0097A7?style=for-the-badge&logo=mediapipe&logoColor=white)
![OpenCV](https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white)
![Flask](https://img.shields.io/badge/Flask-000000?style=for-the-badge&logo=flask&logoColor=white)

### Mobile
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)

</div>

---

## 🚀 Быстрый старт

### Предварительные требования
- Go 1.19+
- Python 3.8+
- PostgreSQL 12+
- Android Studio (для мобильной разработки)

### Пошаговая установка

#### 1️⃣ Настройка базы данных

```bash
# Создайте базу данных
sudo -u postgres psql
CREATE DATABASE lfkdg;
\q

# Примените миграции (скрипт в database/migrations.sql)
psql -d lfkdg -f database/migrations.sql
```

> ⚠️ **Важно**: После создания БД отредактируйте `config/config.go` — укажите свои данные для подключения.

#### 2️⃣ Запуск Go сервера

```bash
cd backend/cmd
go mod download
go run main.go
```
Сервер запустится на `http://localhost:8080`

#### 3️⃣ Запуск Python процессора

```bash
cd backend/python_processor

# Создание виртуального окружения
python3 -m venv venv
source venv/bin/activate  # для Linux/Mac
# или venv\Scripts\activate  # для Windows

# Установка зависимостей
pip install -r requirements.txt

# Запуск процессора
python exercise_detector.py
```
Python сервер запустится на `http://localhost:5000`

#### 4️⃣ Запуск тестового клиента

```bash
# В новом терминале
cd backend/python_processor/debug_frames
source ../venv/bin/activate
python test_client.py
```

---

## 📁 Структура проекта

```
📦 LFK
├── 📂 backend                    # Основной бэкенд на Go
│   ├── 📂 cmd
│   │   └── 📄 main.go            # Точка входа, инициализация сервера
│   │
│   ├── 📂 internal                # Внутренние пакеты
│   │   ├── 📂 auth
│   │   │   └── 📄 jwt.go         # 🔐 Генерация и проверка JWT токенов
│   │   │
│   │   ├── 📂 handlers            # Обработчики HTTP и WebSocket запросов
│   │   │   ├── 📄 exercise_handler.go  # 🏋️ WebSocket для упражнений
│   │   │   ├── 📄 user_handler.go      # 👤 Регистрация, вход, профиль
│   │   │   ├── 📄 workout_handler.go   # 📝 Управление тренировками
│   │   │   └── 📄 stats_handler.go     # 📊 Статистика пользователя
│   │   │
│   │   ├── 📂 middleware
│   │   │   └── 📄 auth_middleware.go  # 🛡️ Проверка JWT для защищенных маршрутов
│   │   │
│   │   ├── 📂 models              # Модели данных
│   │   │   ├── 📄 exercise.go     # Упражнения и обратная связь
│   │   │   ├── 📄 user.go         # Пользователи
│   │   │   ├── 📄 workout_session.go  # Сессии тренировок
│   │   │   ├── 📄 exercise_set.go  # Подходы упражнений
│   │   │   ├── 📄 stats.go        # Статистика
│   │   │   └── 📄 auth_models.go  # Модели аутентификации
│   │   │
│   │   ├── 📂 repository           # Работа с базой данных
│   │   │   ├── 📄 user_repository.go
│   │   │   ├── 📄 exercise_repository.go
│   │   │   ├── 📄 workout_repository.go
│   │   │   └── 📄 stats_repository.go
│   │   │
│   │   └── 📂 websocket
│   │       └── 📄 hub.go          # 🔌 Управление WebSocket подключениями
│   │
│   ├── 📂 pkg
│   │   └── 📂 python_bridge
│   │       └── 📄 client.go       # 🔗 HTTP клиент для Python процессора
│   │
│   ├── 📂 config
│   │   └── 📄 config.go           # ⚙️ Конфигурация приложения
│   │
│   └── 📄 go.mod                   # Зависимости Go
│
├── 📂 python_processor             # Python модуль для компьютерного зрения
│   ├── 📂 exercises
│   │   ├── 📄 __init__.py
│   │   ├── 📄 base_exercise.py     # 🏛️ Базовый класс для всех упражнений
│   │   ├── 📄 fist_exercise.py     # 👊 Упражнение "Кулак"
│   │   ├── 📄 fist_index_exercise.py # ☝️ "Кулак с указательным пальцем"
│   │   ├── 📄 fist_palm_exercise.py # 🤲 "Кулак-ладонь"
│   │   └── 📄 ...                   # Другие упражнения
│   │
│   ├── 📄 exercise_detector.py     # 🎥 Flask сервер, обработка видео через MediaPipe
│   └── 📄 requirements.txt          # Зависимости Python
│
└── 📂 mobile                        # Android приложение (Kotlin)
    └── 📂 app
        └── ...                       # Исходный код Android
```

---

## 🔌 API Endpoints

### Публичные маршруты (не требуют аутентификации)
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `POST` | `/api/register` | Регистрация нового пользователя |
| `POST` | `/api/login` | Вход в систему |
| `GET` | `/api/health` | Проверка статуса сервера |
| `GET` | `/api/user/check` | Проверка существования пользователя |
| `GET` | `/api/user/check/email` | Проверка доступности email |
| `GET` | `/api/user/check/username` | Проверка доступности username |

### Защищенные маршруты (требуют JWT токен в заголовке `Authorization: Bearer <token>`)

#### 👤 Профиль пользователя
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/profile` | Получить профиль пользователя |
| `PUT` | `/api/profile` | Обновить профиль |
| `POST` | `/api/change-password` | Сменить пароль |

#### 🏋️ Упражнения
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/exercises` | Получить список всех упражнений |
| `GET` | `/api/exercises/:id` | Получить информацию об упражнении по ID |
| `GET` | `/api/get_exercise_list` | Получить список упражнений из БД |
| `GET` | `/api/exercise_state` | Получить текущее состояние упражнения |
| `POST` | `/api/exercise/reset` | Сбросить состояние упражнения |

#### 📝 Тренировки
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `POST` | `/api/workout/start` | Начать новую тренировку |
| `POST` | `/api/workout/end` | Завершить текущую тренировку |
| `POST` | `/api/workout/exercise` | Добавить выполненное упражнение |
| `GET` | `/api/workout/history` | Получить историю тренировок |
| `GET` | `/api/workout/current` | Получить информацию о текущей тренировке |
| `GET` | `/api/workout/session/:id` | Получить детали конкретной тренировки |

#### 📊 Статистика
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/stats/overall` | Общая статистика |
| `GET` | `/api/stats/daily` | Статистика за день |
| `GET` | `/api/stats/weekly` | Статистика за неделю |
| `GET` | `/api/stats/monthly` | Статистика за месяц |
| `GET` | `/api/stats/exercises` | Статистика по всем упражнениям |
| `GET` | `/api/stats/exercises/:id` | Статистика по конкретному упражнению |
| `GET` | `/api/stats/period` | Статистика за произвольный период |
| `GET` | `/api/stats/history` | История статистики |

#### 📈 Дашборд
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/dashboard` | Получить данные для дашборда |

### 🔌 WebSocket соединения (требуют JWT токен)
| Эндпоинт | Тип упражнения | Описание |
|----------|----------------|----------|
| `/ws/exercise/fist` | 👊 Кулак | WebSocket для упражнения "Кулак" |
| `/ws/exercise/fist-index` | ☝️ Кулак с указательным пальцем | WebSocket для упражнения "Кулак с указательным пальцем" |
| `/ws/exercise/fist-palm` | 🤲 Кулак-ладонь | WebSocket для упражнения "Кулак-ладонь" |

---

## 🧪 Тестирование

### Запуск тестов Go
```bash
cd backend
go test ./...
```

### Запуск тестов Python
```bash
cd backend/python_processor
pytest tests/
```

---

## 👥 Разработчик

- **Разработчик** - [@gitmonstera](https://github.com/gitmonstera)

---

<div align="center">
  
  [⬆ Вернуться к началу](#lfk-)
  
</div>
