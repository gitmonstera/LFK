package handlers

import (
	"encoding/json"
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

	client := &websocket.Client{
		Hub:        h.hub,
		Conn:       conn,
		Send:       make(chan []byte, 256),
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

	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			log.Printf("readPump error for %s: %v", client.ExerciseID, err)
			break
		}

		log.Printf("Received frame from client %s, size: %d bytes", client.ExerciseID, len(message))

		// Парсим сообщение от клиента
		var clientMsg map[string]interface{}
		if err := json.Unmarshal(message, &clientMsg); err != nil {
			log.Printf("Error parsing client message: %v", err)
			continue
		}

		// Извлекаем frame
		frameData, ok := clientMsg["frame"].(string)
		if !ok {
			log.Printf("No frame data in message")
			continue
		}

		// Отправляем кадр в Python для обработки
		feedback, err := h.processFrame(frameData)
		if err != nil {
			log.Printf("Error processing frame for %s: %v", client.ExerciseID, err)
			// Отправляем сообщение об ошибке клиенту
			errorMsg := map[string]interface{}{
				"status":  "error",
				"message": err.Error(),
			}
			errorJSON, _ := json.Marshal(errorMsg)
			client.Send <- errorJSON
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

func (h *ExerciseHandler) processFrame(frameStr string) (*models.FrameFeedback, error) {
	log.Printf("📤 Отправка в Python, размер данных: %d байт", len(frameStr))

	resp, err := h.pythonClient.ProcessFrame(frameStr)
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
