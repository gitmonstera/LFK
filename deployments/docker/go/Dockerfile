# /home/server/github/lfk/deployments/docker/go/Dockerfile

# Используем официальный образ Go 1.25
FROM golang:1.25-bookworm AS builder

WORKDIR /app

# Копируем go.mod и go.sum
COPY backend/go.mod backend/go.sum ./

# Скачиваем зависимости
RUN go mod download

# Копируем весь код бэкенда
COPY backend/ .

# Собираем приложение (исправленный путь)
RUN go build -o main ./cmd/server/main.go

# Финальный образ
FROM debian:bookworm-slim

RUN apt-get update && apt-get install -y \
    ca-certificates \
    tzdata \
    && rm -rf /var/lib/apt/lists/*

# Создаем пользователя
RUN useradd -m -u 1000 appuser

WORKDIR /app

# Копируем собранное приложение
COPY --from=builder /app/main .

# Копируем конфиги (если нужны)
COPY --from=builder /app/configs ./configs

USER appuser

EXPOSE 8080

CMD ["./main"]
