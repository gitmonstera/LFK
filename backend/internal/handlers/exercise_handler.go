package handlers

import (
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"

	"lfk-backend/internal/models"
	"lfk-backend/internal/websocket"
	"lfk-backend/pkg/python_bridge"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	gorilla "github.com/gorilla/websocket"
)

var upgrader = gorilla.Upgrader{
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
	// Увеличиваем размер буфера для больших сообщений
	ReadBufferSize:  1024 * 1024, // 1MB
	WriteBufferSize: 1024 * 1024, // 1MB
}

type ExerciseHandler struct {
	hub          *websocket.Hub
	pythonClient *python_bridge.Client
	sessions     map[string]*models.ExerciseSession
}

func NewExerciseHandler(hub *websocket.Hub, pythonURL string) *ExerciseHandler {
	return &ExerciseHandler{
		hub:          hub,
		pythonClient: python_bridge.NewClient(pythonURL),
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

	// Устанавливаем большие таймауты
	conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	conn.SetWriteDeadline(time.Now().Add(60 * time.Second))
	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		return nil
	})

	client := &websocket.Client{
		Hub:        h.hub,
		Conn:       conn,
		Send:       make(chan []byte, 512), // Увеличиваем буфер
		ExerciseID: exerciseId,
	}

	client.Hub.Register <- client
	log.Printf("Client registered for exercise: %s", exerciseId)

	go h.readPump(client)
	go h.writePump(client)
}

func (h *ExerciseHandler) readPump(client *websocket.Client) {
	defer func() {
		log.Printf("readPump exiting for client: %s", client.ExerciseID)
		client.Hub.Unregister <- client
		client.Conn.Close()
	}()

	// Устанавливаем лимит на размер сообщения (10MB)
	client.Conn.SetReadLimit(10 * 1024 * 1024)

	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			if gorilla.IsUnexpectedCloseError(err, gorilla.CloseGoingAway, gorilla.CloseAbnormalClosure) {
				log.Printf("readPump error for %s: %v", client.ExerciseID, err)
			}
			break
		}

		log.Printf("Received message from client %s, size: %d bytes", client.ExerciseID, len(message))

		// Отправляем ВСЁ сообщение в Python для обработки
		feedback, err := h.processFrame(string(message))
		if err != nil {
			log.Printf("Error processing frame for %s: %v", client.ExerciseID, err)
			// Отправляем сообщение об ошибке клиенту
			errorMsg := map[string]interface{}{
				"status":  "error",
				"message": err.Error(),
			}
			errorJSON, _ := json.Marshal(errorMsg)
			select {
			case client.Send <- errorJSON:
			default:
				log.Printf("Client %s send buffer full", client.ExerciseID)
			}
			continue
		}

		// Отправляем обратную связь клиенту
		feedbackJSON, _ := json.Marshal(feedback)

		select {
		case client.Send <- feedbackJSON:
			log.Printf("Feedback sent to client %s, size: %d bytes", client.ExerciseID, len(feedbackJSON))
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
				client.Conn.WriteMessage(gorilla.CloseMessage, []byte{})
				return
			}

			// Устанавливаем таймаут на запись
			client.Conn.SetWriteDeadline(time.Now().Add(30 * time.Second))

			// Отправляем сообщение
			if err := client.Conn.WriteMessage(gorilla.TextMessage, message); err != nil {
				log.Printf("writePump error for %s: %v", client.ExerciseID, err)
				return
			}

		case <-ticker.C:
			// Отправляем ping для поддержания соединения
			client.Conn.SetWriteDeadline(time.Now().Add(30 * time.Second))
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				log.Printf("Ping error for %s: %v", client.ExerciseID, err)
				return
			}
		}
	}
}

func (h *ExerciseHandler) processFrame(messageStr string) (*models.FrameFeedback, error) {
	log.Printf("📤 Отправка в Python, размер данных: %d байт", len(messageStr))

	// Пробуем распарсить сообщение от клиента
	var clientMsg map[string]interface{}
	if err := json.Unmarshal([]byte(messageStr), &clientMsg); err != nil {
		log.Printf("❌ Ошибка парсинга сообщения клиента: %v", err)
		return nil, err
	}

	// Проверяем наличие frame
	frameData, ok := clientMsg["frame"].(string)
	if !ok {
		log.Printf("❌ Нет поля frame в сообщении")
		return nil, fmt.Errorf("no frame data")
	}

	// Создаем запрос для Python
	pythonRequest := map[string]interface{}{
		"frame": frameData,
	}

	// Если есть exercise_type, добавляем его
	if exType, ok := clientMsg["exercise_type"]; ok {
		pythonRequest["exercise_type"] = exType
	}

	// Отправляем в Python
	resp, err := h.pythonClient.ProcessFrame(pythonRequest)
	if err != nil {
		log.Printf("❌ Ошибка Python: %v", err)
		return nil, err
	}

	log.Printf("📥 Ответ от Python: status=%s, hand_detected=%v, message='%s', frame_size=%d",
		resp.Status, resp.HandDetected, resp.Message, len(resp.ProcessedFrame))

	feedback := &models.FrameFeedback{
		FistDetected:   resp.FistDetected,
		HandDetected:   resp.HandDetected,
		RaisedFingers:  resp.RaisedFingers,
		FingerStates:   resp.FingerStates,
		Message:        resp.Message,
		ProcessedFrame: resp.ProcessedFrame,
		Timestamp:      time.Now().Unix(),
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
