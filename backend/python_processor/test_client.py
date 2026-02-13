import websocket
import json
import threading
import time

ws_url = "ws://localhost:8080/ws/exercise/test123"

def on_message(ws, message):
    print(f"\n📩 ПОЛУЧЕНО СООБЩЕНИЕ:")
    print(f"Тип: {type(message)}")
    print(f"Длина: {len(message)}")

    try:
        data = json.loads(message)
        print(f"status: {data.get('status')}")
        print(f"hand_detected: {data.get('hand_detected')}")
        print(f"raised_fingers: {data.get('raised_fingers')}")
        print(f"message: {data.get('message')}")
        print(f"frame_size: {len(data.get('processed_frame', ''))}")
        if data.get('processed_frame'):
            print(f"frame_preview: {data.get('processed_frame')[:50]}...")
    except Exception as e:
        print(f"❌ Ошибка парсинга JSON: {e}")
        print(f"Сырые данные: {message[:200]}")

def on_error(ws, error):
    print(f"❌ Ошибка: {error}")

def on_close(ws, close_status_code, close_msg):
    print("🔌 Соединение закрыто")

def on_open(ws):
    print("✅ Соединение открыто")

    def send_frames():
        import cv2
        import base64

        camera = cv2.VideoCapture(0)
        camera.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
        camera.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

        frame_count = 0

        while True:
            good, img = camera.read()
            if not good:
                break

            frame_count += 1

            # Отправляем каждый 5-й кадр
            if frame_count % 5 == 0:
                _, buffer = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 70])
                img_base64 = base64.b64encode(buffer).decode('utf-8')

                ws.send(json.dumps({"frame": img_base64}))
                print(f"📤 Отправлен кадр {frame_count}")

            cv2.imshow('Camera', img)
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break

        camera.release()
        cv2.destroyAllWindows()
        ws.close()

    thread = threading.Thread(target=send_frames)
    thread.start()

if __name__ == "__main__":
    websocket.enableTrace(False)
    print("=" * 60)
    print("🔍 ТЕСТОВЫЙ КЛИЕНТ - СЫРЫЕ ДАННЫЕ")
    print("=" * 60)

    ws = websocket.WebSocketApp(ws_url,
                              on_open=on_open,
                              on_message=on_message,
                              on_error=on_error,
                              on_close=on_close)

    ws.run_forever()