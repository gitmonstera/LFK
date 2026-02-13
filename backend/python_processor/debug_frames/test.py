import cv2
import websocket
import base64
import json
import numpy as np
import time
import sys
import os
import threading
from queue import Queue
import signal

# URL для разных упражнений
EXERCISE_URLS = {
    '1': "ws://localhost:8080/ws/exercise/fist",
    '2': "ws://localhost:8080/ws/exercise/fist-index",
    '3': "ws://localhost:8080/ws/exercise/fist-palm",
}

EXERCISE_NAMES = {
    '1': "Кулак (все пальцы сжаты)",
    '2': "Кулак с указательным пальцем",
    '3': "Кулак-ладонь (кровообращение)",
}

EXERCISE_TYPES = {
    '1': "fist",
    '2': "fist-index",
    '3': "fist-palm",
}

# Цвета для красивого вывода
class Colors:
    HEADER = '\033[95m'
    BLUE = '\033[94m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    CYAN = '\033[96m'
    MAGENTA = '\033[95m'
    END = '\033[0m'
    BOLD = '\033[1m'

running = True
ws = None
camera = None
frame_queue = Queue(maxsize=2)
result_queue = Queue(maxsize=2)
current_exercise_type = "fist"
current_exercise_key = '1'

def signal_handler(sig, frame):
    global running
    print(f"\n{Colors.YELLOW}🛑 Остановка...{Colors.END}")
    running = False
    sys.exit(0)

def clear_screen():
    os.system('cls' if os.name == 'nt' else 'clear')

def print_menu():
    print(f"\n{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}🎮 ВЫБОР УПРАЖНЕНИЯ{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
    for key, name in EXERCISE_NAMES.items():
        print(f"   {Colors.BOLD}{Colors.YELLOW}{key}{Colors.END} - {name}")
    print(f"   {Colors.BOLD}{Colors.RED}q{Colors.END} - Выход")
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")

def display_countdown(countdown, progress, state_name):
    """Отображает красивый отсчет с прогресс-баром"""
    bar_length = 30
    filled = int(progress / 100 * bar_length)
    bar = "█" * filled + "░" * (bar_length - filled)

    print(f"\n{Colors.BOLD}{Colors.CYAN}⏱️  {state_name}{Colors.END}")
    print(f"{Colors.YELLOW}   [{bar}] {countdown}с {progress:.0f}%{Colors.END}")

def display_fist_palm_progress(structured):
    """Отображает прогресс для упражнения Кулак-ладонь"""
    if not structured:
        return

    state = structured.get('state', 'unknown')
    state_name = structured.get('state_name', '')
    current_cycle = structured.get('current_cycle', 0)
    total_cycles = structured.get('total_cycles', 5)
    countdown = structured.get('countdown')
    progress = structured.get('progress_percent', 0)

    # Шаги для отображения
    steps = [
        {"name": "Сожмите кулак", "state": "waiting_fist"},
        {"name": "Держите кулак", "state": "holding_fist"},
        {"name": "Раскройте ладонь", "state": "waiting_palm"},
        {"name": "Держите ладонь", "state": "holding_palm"}
    ]

    print(f"\n{Colors.BOLD}{Colors.CYAN}📋 ПРОГРЕСС УПРАЖНЕНИЯ:{Colors.END}")

    # Определяем текущий шаг
    current_step_index = -1
    for i, step in enumerate(steps):
        if step["state"] == state:
            current_step_index = i
            break

    # Отображаем все шаги
    for i, step in enumerate(steps):
        if i < current_step_index:
            # Пройденные шаги
            print(f"   {Colors.GREEN}✅ {step['name']}{Colors.END}")
        elif i == current_step_index:
            # Текущий шаг
            if "holding" in state and countdown:
                print(f"   {Colors.YELLOW}▶️ {step['name']} [{countdown}с]{Colors.END}")
            else:
                print(f"   {Colors.YELLOW}▶️ {step['name']}{Colors.END}")
        else:
            # Будущие шаги
            print(f"   {Colors.BLUE}⏳ {step['name']}{Colors.END}")

    # Прогресс циклов
    print(f"\n{Colors.MAGENTA}🔄 Цикл: {current_cycle}/{total_cycles}{Colors.END}")

    # Прогресс-бар для удержания
    if "holding" in state and countdown:
        display_countdown(countdown, progress, state_name)

def camera_thread_func():
    global running, camera, frame_queue

    camera = cv2.VideoCapture(0)
    if not camera.isOpened():
        print(f"{Colors.RED}❌ Не удалось открыть камеру{Colors.END}")
        running = False
        return

    camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    while running:
        try:
            good, img = camera.read()
            if not good:
                time.sleep(0.1)
                continue

            img = cv2.resize(img, (320, 240))

            if frame_queue.qsize() < 2:
                frame_queue.put(img)

            time.sleep(0.01)

        except Exception as e:
            print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
            time.sleep(0.5)

    camera.release()

def websocket_thread_func(url, exercise_type):
    global running, ws, result_queue

    print(f"{Colors.CYAN}🔌 Подключение к: {url} с типом {exercise_type}{Colors.END}")

    while running:
        try:
            ws = websocket.create_connection(url, timeout=5)
            print(f"{Colors.GREEN}✅ WebSocket подключен{Colors.END}")

            while running:
                if not frame_queue.empty():
                    img = frame_queue.get()

                    _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 50])
                    img_base64 = base64.b64encode(buffer).decode('utf-8')

                    # Отправляем frame И exercise_type!
                    message = json.dumps({
                        "frame": img_base64,
                        "exercise_type": exercise_type  # ← Важно!
                    })
                    ws.send(message)

                    ws.settimeout(1.0)
                    try:
                        result = ws.recv()
                        data = json.loads(result)
                        result_queue.put(data)
                    except websocket.TimeoutError:
                        pass
                    except Exception as e:
                        print(f"{Colors.RED}❌ Ошибка получения: {e}{Colors.END}")
                        break

                time.sleep(0.01)

            ws.close()

        except Exception as e:
            if running:
                print(f"{Colors.RED}❌ Ошибка WebSocket: {e}{Colors.END}")
                time.sleep(2)

    print(f"{Colors.BLUE}🔌 WebSocket поток завершен{Colors.END}")

def display_thread_func():
    global running, result_queue

    last_display_time = 0
    fps_time = time.time()
    frame_count = 0

    cv2.namedWindow('Original', cv2.WINDOW_NORMAL)
    cv2.namedWindow('Processed', cv2.WINDOW_NORMAL)
    cv2.resizeWindow('Original', 640, 480)
    cv2.resizeWindow('Processed', 640, 480)

    while running:
        try:
            current_time = time.time()

            if not result_queue.empty():
                data = result_queue.get()

                if 'processed_frame' in data and data['processed_frame']:
                    try:
                        frame_bytes = base64.b64decode(data['processed_frame'])
                        nparr = np.frombuffer(frame_bytes, np.uint8)
                        processed = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
                        if processed is not None:
                            processed = cv2.resize(processed, (640, 480))
                            cv2.imshow('Processed', processed)
                    except:
                        pass

                # Обновляем экран при каждом новом сообщении
                clear_screen()

                # Заголовок
                print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
                print(f"{Colors.BOLD}{Colors.GREEN}🎯 {data.get('exercise_name', '')}{Colors.END}")
                print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")

                # Статус руки
                hand = data.get('hand_detected', False)
                print(f"{'🖐️' if hand else '❌'} {'Рука в кадре' if hand else 'Рука не обнаружена'}")

                # Пальцы
                finger_states = data.get('finger_states', [])
                if finger_states:
                    finger_names = ["Б", "У", "С", "Бз", "М"]
                    status = []
                    for i, state in enumerate(finger_states):
                        if state:
                            status.append(f"{Colors.GREEN}{finger_names[i]}⬆️{Colors.END}")
                        else:
                            status.append(f"{Colors.RED}{finger_names[i]}⬇️{Colors.END}")
                    print(f"🖐️ {' | '.join(status)}")

                # СТРУКТУРИРОВАННЫЕ ДАННЫЕ для упражнения Кулак-ладонь
                if 'structured' in data and data['structured']:
                    print(f"\n{Colors.BOLD}{Colors.YELLOW}⭐ ПОЛУЧЕНЫ СТРУКТУРИРОВАННЫЕ ДАННЫЕ!{Colors.END}")
                    display_fist_palm_progress(data['structured'])
                else:
                    print(f"\n{Colors.RED}❌ Нет структурированных данных{Colors.END}")
                    print(f"Ключи в ответе: {list(data.keys())}")

                # Сообщение
                msg = data.get('message', '')
                if msg:
                    if "❌" in msg:
                        print(f"\n{Colors.RED}❌ {msg}{Colors.END}")
                    elif "✅" in msg or "🎉" in msg:
                        print(f"\n{Colors.GREEN}✅ {msg}{Colors.END}")
                    else:
                        print(f"\n{Colors.YELLOW}📢 {msg}{Colors.END}")

                # FPS
                print(f"\n{Colors.BLUE}📹 FPS: {frame_count/(current_time-fps_time+0.001):.1f}{Colors.END}")
                print(f"{Colors.YELLOW}🔹 ESC - выход{Colors.END}")

                last_display_time = current_time

            # Оригинальный кадр
            if not frame_queue.empty():
                img = frame_queue.queue[-1].copy()
                frame_count += 1

                cv2.putText(img, "ESC - выход", (10, 30),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 0), 2)

                img = cv2.resize(img, (640, 480))
                cv2.imshow('Original', img)

            key = cv2.waitKey(1) & 0xFF
            if key == 27:
                running = False
                break

            time.sleep(0.01)

        except Exception as e:
            print(f"{Colors.RED}❌ Ошибка: {e}{Colors.END}")
            time.sleep(0.5)

    cv2.destroyAllWindows()

def run_exercise(exercise_key):
    global running, current_exercise_type, current_exercise_key

    url = EXERCISE_URLS[exercise_key]
    exercise_type = EXERCISE_TYPES[exercise_key]
    current_exercise_type = exercise_type
    current_exercise_key = exercise_key

    clear_screen()
    print(f"{Colors.BOLD}{Colors.GREEN}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.GREEN}🎯 {EXERCISE_NAMES[exercise_key]}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.GREEN}{'='*60}{Colors.END}")
    print(f"{Colors.BLUE}🔌 Подключение к {url}...{Colors.END}")
    print(f"{Colors.BLUE}📋 Тип упражнения: {exercise_type}{Colors.END}")

    while not frame_queue.empty():
        frame_queue.get()
    while not result_queue.empty():
        result_queue.get()

    running = True

    camera_thread = threading.Thread(target=camera_thread_func, daemon=True)
    websocket_thread = threading.Thread(target=websocket_thread_func, args=(url, exercise_type), daemon=True)
    display_thread = threading.Thread(target=display_thread_func, daemon=True)

    camera_thread.start()
    websocket_thread.start()
    display_thread.start()

    display_thread.join()

    running = False

    if ws:
        try:
            ws.close()
        except:
            pass

    print(f"\n{Colors.YELLOW}⏱️ Возврат в меню...{Colors.END}")
    time.sleep(1)

def main():
    signal.signal(signal.SIGINT, signal_handler)

    clear_screen()
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}🎮 ТЕСТОВЫЙ КЛИЕНТ (ЧЕРЕЗ GO){Colors.END}")
    print(f"{Colors.BOLD}{Colors.HEADER}{'='*60}{Colors.END}")

    while True:
        print_menu()
        choice = input("Выберите упражнение (1-3, q - выход): ").strip().lower()

        if choice == 'q':
            print(f"{Colors.BOLD}{Colors.BLUE}👋 До свидания!{Colors.END}")
            break

        if choice in EXERCISE_URLS:
            run_exercise(choice)
        else:
            print(f"{Colors.RED}❌ Неверный выбор{Colors.END}")
            time.sleep(1)

        clear_screen()

if __name__ == "__main__":
    main()