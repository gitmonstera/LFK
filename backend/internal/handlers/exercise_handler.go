package handlers

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"io"
	"log"
	"net/http"
	"strings"
	"time"

	"lfk-backend/internal/models"
	"lfk-backend/internal/websocket"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	gorilla "github.com/gorilla/websocket"
)

var upgrader = gorilla.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

type PythonBridge struct {
	PythonURL string
	client    *http.Client
}

func NewPythonBridge(url string) *PythonBridge {
	return &PythonBridge{
		PythonURL: url,
		client:    &http.Client{Timeout: 10 * time.Second},
	}
}

type ExerciseHandler struct {
	hub          *websocket.Hub
	pythonBridge *PythonBridge
	sessions     map[string]*models.ExerciseSession
}

func NewExerciseHandler(hub *websocket.Hub, pythonBridge *PythonBridge) *ExerciseHandler {
	return &ExerciseHandler{
		hub:          hub,
		pythonBridge: pythonBridge,
		sessions:     make(map[string]*models.ExerciseSession),
	}
}

func (h *ExerciseHandler) HandleWebSocket(w http.ResponseWriter, r *http.Request, exerciseId string) {
	log.Printf("WebSocket connection request for exercise: %s", exerciseId)

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Failed to upgrade connection:", err)
		return
	}

	client := &websocket.Client{
		Hub:        h.hub,
		Conn:       conn,
		Send:       make(chan []byte, 256),
		ExerciseID: exerciseId,
	}

	client.Hub.Register <- client
	log.Printf("Client registered for exercise: %s", exerciseId)

	// Запускаем горутины для чтения и записи
	go h.readPump(client)
	go h.writePump(client)
}

func (h *ExerciseHandler) readPump(client *websocket.Client) {
	defer func() {
		log.Printf("readPump exiting for client: %s", client.ExerciseID)
		client.Hub.Unregister <- client
		client.Conn.Close()
	}()

	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			log.Printf("readPump error for %s: %v", client.ExerciseID, err)
			break
		}

		log.Printf("Received frame from client %s, size: %d bytes", client.ExerciseID, len(message))

		// Отправляем кадр в Python для обработки
		feedback, err := h.processFrame(message)
		if err != nil {
			log.Printf("Error processing frame for %s: %v", client.ExerciseID, err)
			continue
		}

		// Отправляем обратную связь клиенту
		feedbackJSON, _ := json.Marshal(feedback)
		log.Printf("Sending feedback to client %s, size: %d bytes", client.ExerciseID, len(feedbackJSON))

		select {
		case client.Send <- feedbackJSON:
			log.Printf("Feedback sent to client %s", client.ExerciseID)
		default:
			log.Printf("Client %s send buffer full", client.ExerciseID)
		}
	}
}

func (h *ExerciseHandler) writePump(client *websocket.Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		log.Printf("writePump exiting for client: %s", client.ExerciseID)
		ticker.Stop()
		client.Conn.Close()
	}()

	for {
		select {
		case message, ok := <-client.Send:
			if !ok {
				log.Printf("Send channel closed for client %s", client.ExerciseID)
				client.Conn.WriteMessage(gorilla.CloseMessage, []byte{})
				return
			}

			client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.TextMessage, message); err != nil {
				log.Printf("writePump error for %s: %v", client.ExerciseID, err)
				return
			}
			log.Printf("Successfully wrote message to client %s", client.ExerciseID)

		case <-ticker.C:
			client.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				log.Printf("Ping error for %s: %v", client.ExerciseID, err)
				return
			}
		}
	}
}

func (h *ExerciseHandler) processFrame(frame []byte) (*models.FrameFeedback, error) {
	log.Printf("Processing frame, size: %d bytes", len(frame))

	// Проверяем, что данные - это строка base64
	// Иногда данные могут прийти как бинарные, конвертируем в base64 если нужно
	frameStr := string(frame)

	// Проверяем, является ли строка валидным base64
	// Простая проверка: если есть неподходящие символы, возможно это бинарные данные
	isValidBase64 := true
	for _, c := range frameStr {
		if !((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '/' || c == '=') {
			isValidBase64 = false
			break
		}
	}

	// Если это не base64, конвертируем бинарные данные в base64
	if !isValidBase64 {
		log.Printf("Данные не являются base64, конвертируем...")
		frameStr = base64.StdEncoding.EncodeToString(frame)
		log.Printf("Сконвертировали в base64, новый размер: %d", len(frameStr))
	}

	// Добавляем паддинг если нужно
	missingPadding := len(frameStr) % 4
	if missingPadding != 0 {
		frameStr += strings.Repeat("=", 4-missingPadding)
	}

	// Создаем запрос к Python серверу
	reqBody := map[string]interface{}{
		"frame": frameStr,
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		log.Printf("Error marshaling request: %v", err)
		return nil, err
	}

	log.Printf("Sending request to Python server: %s, body size: %d bytes",
		h.pythonBridge.PythonURL+"/process", len(jsonData))

	// Отправляем запрос
	resp, err := h.pythonBridge.client.Post(h.pythonBridge.PythonURL+"/process",
		"application/json",
		bytes.NewBuffer(jsonData))
	if err != nil {
		log.Printf("Python server error: %v", err)
		return nil, err
	}
	defer resp.Body.Close()

	log.Printf("Python server response status: %s", resp.Status)

	// Читаем ответ
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		log.Printf("Error reading Python response: %v", err)
		return nil, err
	}

	log.Printf("Python server response size: %d bytes", len(body))

	// Парсим ответ
	var result map[string]interface{}
	if err := json.Unmarshal(body, &result); err != nil {
		log.Printf("Error unmarshaling Python response: %v", err)
		return nil, err
	}

	// Создаем структуру ответа
	feedback := &models.FrameFeedback{
		FistDetected:   false,
		HandDetected:   false,
		RaisedFingers:  0,
		Message:        "Unknown",
		ProcessedFrame: "",
		Timestamp:      time.Now().Unix(),
	}

	// Заполняем поля из ответа
	if val, ok := result["fist_detected"]; ok {
		if b, ok := val.(bool); ok {
			feedback.FistDetected = b
		}
	}
	if val, ok := result["hand_detected"]; ok {
		if b, ok := val.(bool); ok {
			feedback.HandDetected = b
		}
	}
	if val, ok := result["raised_fingers"]; ok {
		if f, ok := val.(float64); ok {
			feedback.RaisedFingers = int(f)
		}
	}
	if val, ok := result["message"]; ok {
		if s, ok := val.(string); ok {
			feedback.Message = s
		}
	}
	if val, ok := result["processed_frame"]; ok {
		if s, ok := val.(string); ok {
			feedback.ProcessedFrame = s
		}
	}

	return feedback, nil
}

func (h *ExerciseHandler) StartExercise(c *gin.Context) {
	var req models.ExerciseRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	session := &models.ExerciseSession{
		ID:        uuid.New().String(),
		UserID:    "user-1",
		Type:      req.ExerciseType,
		StartTime: time.Now(),
		IsActive:  true,
	}

	h.sessions[session.ID] = session
	log.Printf("Started exercise session: %s", session.ID)
	c.JSON(http.StatusOK, session)
}

func (h *ExerciseHandler) StopExercise(c *gin.Context) {
	var req struct {
		SessionID string `json:"sessionId" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	session, exists := h.sessions[req.SessionID]
	if !exists {
		c.JSON(http.StatusNotFound, gin.H{"error": "Session not found"})
		return
	}

	now := time.Now()
	session.EndTime = &now
	session.IsActive = false

	log.Printf("Stopped exercise session: %s", session.ID)
	c.JSON(http.StatusOK, session)
}
