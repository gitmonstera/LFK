# Берем официальный образ Python
FROM python:3.10-slim

# Устанавливаем системные зависимости для OpenCV и MediaPipe
# Обновляем список пакетов
RUN apt-get update && apt-get install -y \
    libgl1-mesa-glx \        # для OpenCV (графика) \
    libglib2.0-0 \           # системная библиотека \
    libsm6 \                  # для работы с изображениями \
    libxext6 \                # X11 расширения \
    libxrender-dev \          # для рендеринга \
    libgomp1 \                # OpenMP параллельные вычисления \
    && rm -rf /var/lib/apt/lists/*  # чистим кэш (уменьшаем размер)

# Создаем рабочую папку
WORKDIR /app

# Сначала копируем только requirements.txt (для кэширования)
COPY python_processor/requirements.txt .

# Устанавливаем Python зависимости
RUN pip install --no-cache-dir -r requirements.txt

# Теперь копируем весь код
COPY python_processor/ .

# Создаем пользователя (безопасность!)
RUN useradd -m -u 1000 appuser && chown -R appuser:appuser /app

# Переключаемся на него
USER appuser

# Указываем порт
EXPOSE 5001

# Запускаем (PYTHONUNBUFFERED - чтобы логи выводились сразу)
CMD ["python", "-u", "exercise_detector.py"]