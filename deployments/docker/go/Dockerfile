# ---- 1 СЛОЙ: Сборка приложения ----
# Берем образ с Go для компиляции
FROM golang:1.21-alpine AS builder

# Устанавливаем рабочую папку
WORKDIR /app

# Копируем файлы с зависимостями (для кэширования)
COPY backend/go.mod backend/go.sum ./

# Скачиваем все зависимости
RUN go mod download

# Копируем весь код бэкенда
COPY backend/ .

# Компилируем в один бинарный файл
# CGO_ENABLED=0 - отключаем CGO для статической сборки
# -ldflags="-w -s" - уменьшаем размер бинарника
RUN CGO_ENABLED=0 GOOS=linux go build -ldflags="-w -s" -o lfk-server ./cmd/main.go

# ---- 2 СЛОЙ: Финальный образ ----
# Берем минимальный образ Alpine
FROM alpine:latest

# Устанавливаем необходимые пакеты
# ca-certificates - для HTTPS запросов
# tzdata - для временных зон
# bash - для скриптов (опционально)
RUN apk --no-cache add ca-certificates tzdata bash

# Создаем непривилегированного пользователя (безопасность!)
RUN adduser -D -u 1000 appuser

# Переключаемся на него
USER appuser

# Создаем рабочую папку
WORKDIR /app

# Копируем бинарник из первого слоя
COPY --from=builder /app/lfk-server .

# Копируем конфиги (они будут перезаписываться через volume)
COPY --from=builder /app/configs ./configs

# Указываем порт
EXPOSE 8080

# Запускаем сервер
CMD ["./lfk-server"]