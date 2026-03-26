# Лечебная Физическая Культура (ЛФК) 🏥

<div align="center">
  <img src="resours/center_logo.svg" alt="LFK Logo" width="200"/>
</div>

<div align="center">
  
  ![Version](https://img.shields.io/badge/version-2.0.0-blue?style=flat-square)
  ![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
  [![Go Report Card](https://goreportcard.com/badge/github.com/gitmonstera/lfk?style=flat-square)](https://goreportcard.com/report/github.com/gitmonstera/lfk)
  ![Quasar](https://img.shields.io/badge/Quasar-1976D2?style=flat-square&logo=quasar&logoColor=white)
  ![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?style=flat-square&logo=vue.js&logoColor=white)
  ![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)
  ![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)
  
  *Умный помощник для выполнения упражнений лечебной физкультуры*
  
  [О проекте](#-о-проекте) •
  [Технологии](#-технологический-стек) •
  [Быстрый старт](#-быстрый-старт) •
  [Структура](#-структура-проекта) •
  [API](#-api-endpoints) •
  [Скриншоты](#-интерфейс-приложения)
  
</div>

---

## 📋 О проекте

**LFK** — это интеллектуальная система, которая помогает людям правильно выполнять упражнения лечебной физкультуры (ЛФК). Проект использует компьютерное зрение для анализа движений в реальном времени, предоставляет голосовую обратную связь и отслеживает прогресс пользователя.

### ✨ Ключевые возможности

| | | |
|---|---|---|
| 📹 **Компьютерное зрение** | Анализ движений в реальном времени через камеру смартфона или веб-камеру |
| 📊 **Детальная статистика** | Отслеживание прогресса, история тренировок, достижения |
| 📱 **Кроссплатформенность** | Веб-версия (Quasar) и мобильное приложение (Kotlin) |
| 🔄 **WebSocket соединение** | Мгновенная передача видео и обратная связь в реальном времени |
| 🏋️ **Разнообразие упражнений** | Кулак, кулак-ладонь, кулак с указательным пальцем, считалочка и другие |

---

## 🛠 Технологический стек

<div align="center">

### Backend
![Go](https://img.shields.io/badge/Go-00ADD8?style=for-the-badge&logo=go&logoColor=white)
![Python](https://img.shields.io/badge/Python-3776AB?style=for-the-badge&logo=python&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-010101?style=for-the-badge&logo=socket.io&logoColor=white)

### Frontend
![Quasar](https://img.shields.io/badge/Quasar-1976D2?style=for-the-badge&logo=quasar&logoColor=white)
![Vue.js](https://img.shields.io/badge/Vue.js-4FC08D?style=for-the-badge&logo=vue.js&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![SCSS](https://img.shields.io/badge/SCSS-CC6699?style=for-the-badge&logo=sass&logoColor=white)

### Computer Vision
![MediaPipe](https://img.shields.io/badge/MediaPipe-0097A7?style=for-the-badge&logo=mediapipe&logoColor=white)
![OpenCV](https://img.shields.io/badge/OpenCV-5C3EE8?style=for-the-badge&logo=opencv&logoColor=white)

### Mobile
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpack-compose&logoColor=white)

### DevOps
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-009639?style=for-the-badge&logo=nginx&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/GitHub%20Actions-2088FF?style=for-the-badge&logo=github-actions&logoColor=white)

</div>

---

## 📱 Интерфейс приложения

<div align="center">

### 🔐 Авторизация

| | | |
|:---:|:---:|:---:|
| **Вход** | **Регистрация** | **Главная** |
| <img src="resours/screen/LoginScreen.jpg" width="200"/> | <img src="resours/screen/RegisterScreen.jpg" width="200"/> | <img src="resours/screen/MainMenuScreen.jpg" width="200"/> |

### 👤 Профиль и статистика

| | | |
|:---:|:---:|:---:|
| **Профиль** | **Общая статистика** | **Статистика за день** |
| <img src="resours/screen/ProfileScreen.jpg" width="200"/> | <img src="resours/screen/StatsScreen1.jpg" width="200"/> | <img src="resours/screen/StatsScreen2.jpg" width="200"/> |

| | | |
|:---:|:---:|:---:|
| **Недельная статистика** | **Месячная статистика** | **Упражнения** |
| <img src="resours/screen/StatsScreen3.jpg" width="200"/> | <img src="resours/screen/StatsScreen4.jpg" width="200"/> | <img src="resours/screen/StatsScreen5.jpg" width="200"/> |

### 🎯 Выполнение упражнения

| | | |
|:---:|:---:|:---:|
| **Выбор упражнения** | **Процесс выполнения** | **Результат** |
| <img src="resours/screen/ExercisesScreen.jpg" width="200"/> | <img src="resours/screen/ExerciseScreen.jpg" width="200"/> | <img src="resours/screen/StatsScreen6.jpg" width="200"/> |

</div>

---

## 🚀 Быстрый старт

### Предварительные требования
- Go 1.21+
- Python 3.10+
- PostgreSQL 14+
- Redis 7+
- Node.js 18+ (для фронтенда)
- Docker & Docker Compose (опционально)

### 🏃‍♂️ Запуск в режиме разработки

#### 1️⃣ Клонирование репозитория
```bash
git clone https://github.com/gitmonstera/lfk.git
cd lfk
```

#### 2️⃣ Настройка базы данных
```bash
# Создайте базу данных
sudo -u postgres psql
CREATE DATABASE lfkdg;
\q

# Примените миграции
psql -d lfkdg -f backend/migrations/init.sql
```

#### 3️⃣ Настройка Redis
```bash
sudo apt update
sudo apt install redis-server
sudo systemctl start redis-server
redis-cli ping  # Должно вернуть: PONG
```

#### 4️⃣ Запуск бэкенда
```bash
# Терминал 1 - Go сервер
cd backend
go mod download
go run cmd/main.go
# Сервер запустится на http://localhost:9000

# Терминал 2 - Python процессор
cd backend/python_processor
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python exercise_detector.py
```

#### 5️⃣ Запуск фронтенда
```bash
# Терминал 3 - Quasar
cd frontend
npm install
npm run dev
# Сайт будет доступен на http://localhost:8080
```

### 🐳 Запуск через Docker Compose

```bash
# Сборка и запуск всех сервисов
docker-compose up -d --build

# Проверка статуса
docker-compose ps

# Просмотр логов
docker-compose logs -f
```

---

## 📁 Структура проекта

```
📦 LFK
├── 🤖 androidApp
│   └── 📱 app
│
├── ⚙️ backend
│   ├── 🚀 cmd
│   │   └── server
│   │       ├── ⚙️ configs
│   │       │   └── 📄 config.yaml
│   │       ├── 🌐 web
│   │       │   ├── 🎨 assets
│   │       │   │   ├── 📄 DownloadPage-B7PzhjNf.css
│   │       │   │   ├── 📄 DownloadPage-CCmhotBU.js
│   │       │   │   ├── 📄 ExercisePage-jUDrZysy.css
│   │       │   │   ├── 📄 ExercisePage-RjrWflDj.js
│   │       │   │   ├── 📄 ExercisesPage-BQC3bGHU.js
│   │       │   │   ├── 📄 ExercisesPage-wQCisriw.css
│   │       │   │   ├── 📄 index-CT6VCLMC.js
│   │       │   │   ├── 📄 index-dL11cRdX.css
│   │       │   │   ├── 📄 index-DxZe7dCM.js
│   │       │   │   ├── 📄 IndexPage-5hXwcDwN.js
│   │       │   │   ├── 📄 IndexPage-Ci1gmyRV.css
│   │       │   │   ├── 📄 LoginPage-0cnyOmuO.js
│   │       │   │   ├── 📄 LoginPage-BYX1jlvN.css
│   │       │   │   ├── 📄 MainLayout-6xgF5b3Y.css
│   │       │   │   ├── 📄 MainLayout-DT1RSESP.js
│   │       │   │   ├── 📄 ProfileLayout-CpC4vt6W.js
│   │       │   │   ├── 📄 ProfileLayout-CXE_Pt5u.css
│   │       │   │   ├── 📄 ProfilePage-B8cwzaaR.css
│   │       │   │   ├── 📄 ProfilePage-BXHzCIto.js
│   │       │   │   └── 📄 ...
│   │       │   ├── 📥 downloads
│   │       │   │   └── 📄 lfk-android.apk
│   │       │   ├── 🎯 logo
│   │       │   │   └── 📄 logo.svg
│   │       │   └── 📄 index.html
│   │       ├── 📄 index.html
│   │       └── 📄 main.go
│   │
│   ├── 🔧 internal
│   │   ├── 🔐 auth
│   │   │   └── 📄 jwt.go
│   │   ├── ⚙️ config
│   │   │   └── 📄 config.go
│   │   ├── 🖐️ handlers
│   │   │   ├── 📄 exercise_handler.go
│   │   │   ├── 📄 stats_handler.go
│   │   │   ├── 📄 user_handler.go
│   │   │   └── 📄 workout_handler.go
│   │   ├── 🛡️ middleware
│   │   │   └── 📄 auth_middleware.go
│   │   ├── 📊 models
│   │   │   ├── 📄 auth_models.go
│   │   │   ├── 📄 exercise.go
│   │   │   ├── 📄 exercise_set.go
│   │   │   ├── 📄 register_request.go
│   │   │   ├── 📄 stats.go
│   │   │   ├── 📄 user.go
│   │   │   └── 📄 workout_session.go
│   │   ├── 📦 redis
│   │   │   ├── 📄 client.go
│   │   │   ├── 📄 pubsub.go
│   │   │   └── 📄 queue.go
│   │   ├── 🗄️ repository
│   │   │   ├── 📄 exercise_repository.go
│   │   │   ├── 📄 stats_repository.go
│   │   │   ├── 📄 user_repository.go
│   │   │   └── 📄 workout_repository.go
│   │   └── 🔌 websocket
│   │       ├── 📄 cluster_hub.go
│   │       └── 📄 hub.go
│   │
│   ├── 🗃️ migrations
│   │   └── 📄 init.sql
│   ├── 📦 pkg
│   │   └── python_bridge
│   │       └── 📄 client.go
│   ├── 🐍 python_processor
│   │   ├── 🏋️ exercises
│   │   │   ├── 📄 base_exercise.py
│   │   │   ├── 📄 finger_touching_exercise.py
│   │   │   ├── 📄 fist_exercise.py
│   │   │   ├── 📄 fist_index_exercise.py
│   │   │   ├── 📄 fist_palm_exercise.py
│   │   │   └── 📄 __init__.py
│   │   ├── 📄 exercise_detector.py
│   │   └── 📄 requirements.txt
│   ├── 📄 go.mod
│   └── 📄 go.sum
│
├── 🐳 deployments
│   ├── 🐋 docker
│   │   ├── go
│   │   │   └── 📄 Dockerfile
│   │   └── python
│   │       └── 📄 Dockerfile
│   └── 📄 docker-compose.yml
│
├── 🎨 resours
│   ├── 📱 screen
│   │   ├── 📄 LoginScreen.jpg
│   │   ├── 📄 MainMenuScreen.jpg
│   │   ├── 📄 ProfileScreen.jpg
│   │   ├── 📄 RegisterScreen.jpg
│   │   ├── 📄 StatsScreen1.jpg
│   │   ├── 📄 StatsScreen2.jpg
│   │   ├── 📄 StatsScreen3.jpg
│   │   ├── 📄 StatsScreen4.jpg
│   │   └── 📄 StatsScreen5.jpg
│   ├── 📄 center_logo.svg
│   └── 📄 lfk_logo_v1.svg
│
├── 📜 scripts
├── 📄 create_tables.sql
└── 📖 README.md

66 directories, 165 files
```

---

## 🔌 API Endpoints

<details>
<summary>📋 Полный список API эндпоинтов</summary>

### Публичные маршруты
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `POST` | `/api/register` | Регистрация нового пользователя |
| `POST` | `/api/login` | Вход в систему |
| `GET` | `/api/health` | Проверка статуса сервера |

### Защищенные маршруты (JWT)

#### 👤 Профиль
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/profile` | Получить профиль |
| `PUT` | `/api/profile` | Обновить профиль |

#### 🏋️ Упражнения
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/get_exercise_list` | Список всех упражнений |
| `GET` | `/api/exercise_state` | Состояние упражнения |
| `POST` | `/api/exercise/reset` | Сброс упражнения |

#### 📝 Тренировки
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `POST` | `/api/workout/start` | Начать тренировку |
| `POST` | `/api/workout/end` | Завершить тренировку |
| `POST` | `/api/workout/exercise` | Добавить выполненное упражнение |
| `GET` | `/api/workout/history` | История тренировок |

#### 📊 Статистика
| Метод | Эндпоинт | Описание |
|--------|----------|----------|
| `GET` | `/api/stats/overall` | Общая статистика |
| `GET` | `/api/stats/daily` | Статистика за день |
| `GET` | `/api/stats/weekly` | Статистика за неделю |
| `GET` | `/api/stats/monthly` | Статистика за месяц |
| `GET` | `/api/stats/exercises` | Статистика по упражнениям |

### 🔌 WebSocket
| Эндпоинт | Описание |
|----------|----------|
| `/ws/exercise/fist` | Упражнение "Кулак" |
| `/ws/exercise/fist-index` | Упражнение "Кулак с указательным пальцем" |
| `/ws/exercise/fist-palm` | Упражнение "Кулак-ладонь" |

</details>

---

## 📦 Сборка для production

### Фронтенд
```bash
cd frontend
npm run build
# Собранные файлы в папке dist/
```

### Бэкенд
```bash
cd backend
go build -o lfk-server cmd/main.go
```

### Полный production запуск

 На сервере 80.93.63.206
    Сайт: http://80.93.63.206:8080

---

## 🤝 Как внести вклад

1. Форкните репозиторий
2. Создайте ветку (`git checkout -b feature/amazing-feature`)
3. Закоммитьте изменения (`git commit -m 'Add amazing feature'`)
4. Запушьте (`git push origin feature/amazing-feature`)
5. Откройте Pull Request

---

## 👥 Разработчик

- **Full-stack Developer** - [@gitmonstera](https://github.com/gitmonstera)

---

<div align="center">

### 🌟 Если проект вам полезен, поставьте звездочку! 🌟

**[⬆ Вернуться к началу](#лечебная-физическая-культура-лфк)**
