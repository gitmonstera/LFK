#!/usr/bin/env python3
"""
ФИНАЛЬНЫЙ ТЕСТ: 2 одновременных пользователя
Сервер: 192.168.0.164
"""

import base64
import json
import threading
import time
import requests
import websocket
import numpy as np
from datetime import datetime
import cv2
import io

# ============= НАСТРОЙКИ =============
SERVER_URL = "http://192.168.0.164:8080"
WS_URL = "ws://192.168.0.164:8080/ws/exercise/fist-palm"

# Тестовые пользователи (должны существовать в БД!)
TEST_USERS = [
    {"email": "q@q.com", "password": "password", "name": "Пользователь 1"},
    {"email": "w@w.com", "password": "password", "name": "Пользователь 2"}
]

EXERCISE_TYPE = "fist-palm"
TEST_DURATION = 20  # секунд
FRAME_RATE = 5      # кадров в секунду
# =====================================

class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    CYAN = '\033[96m'
    END = '\033[0m'
    BOLD = '\033[1m'

def log(user_name, message, color=Colors.END):
    """Цветной лог с временем"""
    timestamp = datetime.now().strftime("%H:%M:%S.%f")[:-3]
    print(f"{color}[{timestamp}] [{user_name}] {message}{Colors.END}")

class TestUser:
    def __init__(self, user_data):
        self.email = user_data["email"]
        self.password = user_data["password"]
        self.name = user_data["name"]
        self.token = None
        self.session_id = None
        self.ws = None
        self.frames_sent = 0
        self.frames_received = 0
        self.errors = 0
        self.timeouts = 0
        self.running = False
        self.stats = {'hand_detected': 0}
        self.frame_counter = 0

    def generate_frame(self):
        """Генерирует тестовый кадр"""
        img = np.zeros((240, 320, 3), dtype=np.uint8)
        self.frame_counter += 1

        # Рисуем простую фигуру
        cv2.circle(img, (160, 120), 50, (255, 255, 255), -1)
        cv2.putText(img, self.name, (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 255), 2)

        _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 70])
        return base64.b64encode(buffer).decode('utf-8')

    def login(self):
        try:
            r = requests.post(f"{SERVER_URL}/api/login",
                              json={"email": self.email, "password": self.password},
                              timeout=5)
            if r.status_code == 200:
                self.token = r.json()["token"]
                log(self.name, f"✅ Успешный вход", Colors.GREEN)
                return True
        except Exception as e:
            log(self.name, f"❌ Ошибка входа: {e}", Colors.RED)
        return False

    def start_workout(self):
        try:
            r = requests.post(f"{SERVER_URL}/api/workout/start",
                              headers={"Authorization": f"Bearer {self.token}"},
                              timeout=5)
            if r.status_code == 200:
                self.session_id = r.json()['id']
                log(self.name, f"✅ Тренировка начата", Colors.GREEN)
                return True
        except Exception as e:
            log(self.name, f"❌ Ошибка: {e}", Colors.RED)
        return False

    def connect_websocket(self):
        try:
            self.ws = websocket.create_connection(
                f"{WS_URL}?token={self.token}",
                timeout=10
            )
            log(self.name, f"✅ WebSocket подключен", Colors.GREEN)
            return True
        except Exception as e:
            log(self.name, f"❌ WebSocket ошибка: {e}", Colors.RED)
            return False

    def send_frame(self):
        try:
            frame = self.generate_frame()
            self.ws.send(json.dumps({
                "frame": frame,
                "exercise_type": EXERCISE_TYPE
            }))
            self.frames_sent += 1

            self.ws.settimeout(1.0)
            try:
                result = self.ws.recv()
                self.frames_received += 1
                data = json.loads(result)
                if data.get('hand_detected'):
                    self.stats['hand_detected'] += 1
            except websocket.WebSocketTimeoutException:
                self.timeouts += 1

            return True
        except Exception as e:
            self.errors += 1
            return False

    def run_test(self, duration):
        self.running = True
        start = time.time()
        frame_count = 0

        log(self.name, f"▶️ Запуск на {duration} сек", Colors.CYAN)

        while self.running and (time.time() - start) < duration:
            self.send_frame()
            frame_count += 1
            time.sleep(1.0 / FRAME_RATE)

            if frame_count % 10 == 0:
                success = self.frames_received / self.frames_sent * 100
                log(self.name, f"📊 Прогресс: {success:.1f}%", Colors.YELLOW)

        self.cleanup()
        self.print_stats()

    def cleanup(self):
        self.running = False
        if self.ws:
            try:
                self.ws.close()
            except:
                pass
        log(self.name, f"🧹 Завершено", Colors.BLUE)

    def print_stats(self):
        success = self.frames_received / self.frames_sent * 100 if self.frames_sent else 0
        log(self.name, f"📊 СТАТИСТИКА:", Colors.BOLD)
        log(self.name, f"   Отправлено: {self.frames_sent}", Colors.CYAN)
        log(self.name, f"   Получено: {self.frames_received}", Colors.CYAN)
        log(self.name, f"   Успех: {success:.1f}%", Colors.GREEN if success > 90 else Colors.YELLOW)
        log(self.name, f"   Таймаутов: {self.timeouts}", Colors.YELLOW)
        log(self.name, f"   Ошибок: {self.errors}", Colors.RED if self.errors else Colors.GREEN)

def main():
    print("=" * 60)
    print("🚀 ТЕСТ: 2 ПОЛЬЗОВАТЕЛЯ ОДНОВРЕМЕННО")
    print("=" * 60)
    print(f"Сервер: {SERVER_URL}")
    print(f"Длительность: {TEST_DURATION} сек")
    print(f"Пользователей: {len(TEST_USERS)}")
    print("=" * 60)

    users = []
    for user_data in TEST_USERS:
        user = TestUser(user_data)
        if not user.login():
            print(f"❌ Ошибка авторизации {user_data['name']}")
            return
        if not user.start_workout():
            print(f"❌ Ошибка старта {user_data['name']}")
            return
        if not user.connect_websocket():
            print(f"❌ Ошибка WebSocket {user_data['name']}")
            return
        users.append(user)

    print("\n" + "=" * 60)
    print("✅ ВСЕ ГОТОВЫ! ЗАПУСК...")
    print("=" * 60)

    threads = []
    for user in users:
        t = threading.Thread(target=user.run_test, args=(TEST_DURATION,))
        t.start()
        threads.append(t)
        time.sleep(1)

    for t in threads:
        t.join()

    print("\n" + "=" * 60)
    print("🏁 ТЕСТ ЗАВЕРШЕН")
    print("=" * 60)

    total_sent = sum(u.frames_sent for u in users)
    total_recv = sum(u.frames_received for u in users)
    total_errors = sum(u.errors for u in users)
    total_timeouts = sum(u.timeouts for u in users)

    print(f"\n📊 ИТОГИ:")
    print(f"   Всего кадров отправлено: {total_sent}")
    print(f"   Всего ответов получено: {total_recv}")
    if total_sent > 0:
        print(f"   Общий успех: {total_recv/total_sent*100:.1f}%")
    print(f"   Таймаутов: {total_timeouts}")
    print(f"   Ошибок: {total_errors}")

    if total_errors == 0 and total_timeouts < total_sent * 0.1:
        print(f"\n{Colors.GREEN}✅ ТЕСТ ПРОЙДЕН!{Colors.END}")
    else:
        print(f"\n{Colors.YELLOW}⚠️ ЕСТЬ ПОТЕРИ, НУЖНА ОПТИМИЗАЦИЯ{Colors.END}")

if __name__ == "__main__":
    main()