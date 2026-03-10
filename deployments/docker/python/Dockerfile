# /home/server/github/lfk/deployments/docker/python/Dockerfile

FROM python:3.10-slim

# Устанавливаем системные зависимости для OpenCV и MediaPipe
# Используем libgl1 вместо libgl1-mesa-glx (более новая версия)
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libgomp1 \
    libgl1-mesa-dri \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем requirements.txt
COPY backend/python_processor/requirements.txt .

# Устанавливаем Python зависимости
RUN pip install --no-cache-dir -r requirements.txt

# Копируем весь код
COPY backend/python_processor/ .

# Создаем пользователя для безопасности
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app
USER appuser

# Указываем порт
EXPOSE 5001

# Запускаем приложение
CMD ["python", "-u", "exercise_detector.py"]
