import websocket
import json
import time

def on_message(ws, message):
    print(f"Received: {message}")

def on_error(ws, error):
    print(f"Error: {error}")

def on_close(ws, close_status_code, close_msg):
    print("Connection closed")

def on_open(ws):
    print("Connection opened")
    # Отправляем тестовое сообщение
    ws.send(json.dumps({"test": "hello"}))

if __name__ == "__main__":
    # Получите токен сначала через login
    import requests
    login_resp = requests.post(
        "http://localhost:8080/api/login",
        json={"email": "e@e.com", "password": "password"}
    )
    token = login_resp.json()["token"]

    ws_url = f"ws://localhost:8080/ws/exercise/fist-palm?token={token}"
    print(f"Connecting to {ws_url}")

    ws = websocket.WebSocketApp(ws_url,
                                on_open=on_open,
                                on_message=on_message,
                                on_error=on_error,
                                on_close=on_close)

    ws.run_forever()