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
	ReadBufferSize:  1024 * 1024,
	WriteBufferSize: 1024 * 1024,
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

// GetExerciseState - возвращает текущее состояние упражнения
func (h *ExerciseHandler) GetExerciseState(c *gin.Context) {
	exerciseType := c.Query("type")
	if exerciseType == "" {
		exerciseType = "fist-palm" // значение по умолчанию
	}

	log.Printf("📊 Запрос состояния упражнения: %s", exerciseType)

	// Создаем запрос к Python серверу для получения состояния
	stateRequest := map[string]interface{}{
		"exercise_type":  exerciseType,
		"get_state_only": true,
	}

	resp, err := h.pythonClient.ProcessFrame(stateRequest)
	if err != nil {
		log.Printf("❌ Ошибка получения состояния от Python: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":   "Failed to get exercise state",
			"details": err.Error(),
		})
		return
	}

	// Формируем ответ
	response := gin.H{
		"status":           "success",
		"current_exercise": resp.CurrentExercise,
		"exercise_name":    resp.ExerciseName,
		"auto_reset":       false,
	}

	// Добавляем структурированные данные если есть
	if resp.Structured != nil {
		response["structured"] = gin.H{
			"state":            resp.Structured.State,
			"state_name":       resp.Structured.StateName,
			"current_cycle":    resp.Structured.Cycle,
			"total_cycles":     resp.Structured.TotalCycles,
			"completed":        resp.Structured.Completed,
			"countdown":        resp.Structured.Countdown,
			"progress_percent": resp.Structured.Progress,
			"message":          resp.Structured.Message,
			"step":             resp.Structured.Step,
			"step_name":        resp.Structured.StepName,
		}

		// Обновляем флаг auto_reset
		response["auto_reset"] = resp.Structured.AutoReset
	}

	log.Printf("📊 Состояние упражнения: cycle=%d, completed=%v",
		resp.Structured.Cycle, resp.Structured.Completed)
	c.JSON(http.StatusOK, response)
}

// ResetExercise - универсальный сброс упражнения
func (h *ExerciseHandler) ResetExercise(c *gin.Context) {
	var req struct {
		ExerciseType string `json:"exercise_type" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		log.Printf("❌ Ошибка парсинга запроса: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{
			"error":   "Invalid request format",
			"details": err.Error(),
		})
		return
	}

	log.Printf("🔄 Получен запрос на сброс упражнения: %s", req.ExerciseType)

	// Отправляем команду сброса в Python процессор
	resetRequest := map[string]interface{}{
		"exercise_type":         req.ExerciseType,
		"reset_for_new_attempt": true,
	}

	resp, err := h.pythonClient.ProcessFrame(resetRequest)
	if err != nil {
		log.Printf("❌ Ошибка при сбросе упражнения в Python: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":   "Failed to reset exercise",
			"details": err.Error(),
		})
		return
	}

	response := gin.H{
		"status":           "success",
		"message":          "Exercise reset successfully",
		"current_exercise": resp.CurrentExercise,
		"exercise_name":    resp.ExerciseName,
	}

	if resp.Structured != nil {
		response["structured"] = gin.H{
			"state":            resp.Structured.State,
			"state_name":       resp.Structured.StateName,
			"current_cycle":    resp.Structured.Cycle,
			"total_cycles":     resp.Structured.TotalCycles,
			"completed":        resp.Structured.Completed,
			"countdown":        resp.Structured.Countdown,
			"progress_percent": resp.Structured.Progress,
			"message":          resp.Structured.Message,
			"step":             resp.Structured.Step,
			"step_name":        resp.Structured.StepName,
		}
	}

	c.JSON(http.StatusOK, response)
}

func (h *ExerciseHandler) HandleWebSocket(w http.ResponseWriter, r *http.Request, exerciseId string) {
	log.Printf("WebSocket connection request for exercise: %s", exerciseId)

	log.Printf("=== WEBSOCKET HANDLER ===")
	log.Printf("Exercise ID: %s", exerciseId)
	log.Printf("URL: %s", r.URL.String())
	log.Printf("Token from query: %s", r.URL.Query().Get("token"))

	// Проверяем, прошел ли пользователь через middleware
	userID := r.Context().Value("user_id")
	log.Printf("User ID from context: %v", userID)

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Println("Failed to upgrade connection:", err)
		return
	}

	err = conn.SetReadDeadline(time.Now().Add(60 * time.Second))
	if err != nil {
		return
	}
	err = conn.SetWriteDeadline(time.Now().Add(60 * time.Second))
	if err != nil {
		return
	}
	conn.SetPongHandler(func(string) error {
		err := conn.SetReadDeadline(time.Now().Add(60 * time.Second))
		if err != nil {
			return err
		}
		return nil
	})

	client := &websocket.Client{
		Hub:        h.hub,
		Conn:       conn,
		Send:       make(chan []byte, 512),
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
		err := client.Conn.Close()
		if err != nil {
			return
		}
	}()

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

		// Парсим сообщение клиента
		var clientMsg map[string]interface{}
		if err := json.Unmarshal(message, &clientMsg); err != nil {
			log.Printf("Error parsing client message: %v", err)
			continue
		}

		// Проверяем, есть ли запрос на сброс
		if reset, ok := clientMsg["reset_for_new_attempt"]; ok && reset == true {
			log.Printf("🔄 Получен запрос на сброс упражнения через WebSocket")

			// Отправляем команду сброса в Python
			resetRequest := map[string]interface{}{
				"exercise_type":         client.ExerciseID,
				"reset_for_new_attempt": true,
			}

			feedback, err := h.processFrameWithReset(resetRequest)
			if err != nil {
				log.Printf("Error processing reset: %v", err)
			} else {
				feedbackJSON, _ := json.Marshal(feedback)
				select {
				case client.Send <- feedbackJSON:
					log.Printf("Reset confirmation sent to client")
				default:
				}
			}
			continue
		}

		feedback, err := h.processFrame(string(message))
		if err != nil {
			log.Printf("Error processing frame for %s: %v", client.ExerciseID, err)
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

		feedbackJSON, _ := json.Marshal(feedback)

		select {
		case client.Send <- feedbackJSON:
			log.Printf("Feedback sent to client %s, size: %d bytes, structured=%v",
				client.ExerciseID, len(feedbackJSON), feedback.Structured != nil)
		default:
			log.Printf("Client %s send buffer full", client.ExerciseID)
		}
	}
}

// Новый метод для обработки сброса
func (h *ExerciseHandler) processFrameWithReset(request map[string]interface{}) (*models.FrameFeedback, error) {
	log.Printf("🔄 Отправка команды сброса в Python")

	resp, err := h.pythonClient.ProcessFrame(request)
	if err != nil {
		log.Printf("❌ Ошибка Python при сбросе: %v", err)
		return nil, err
	}

	// Конвертируем StructuredData для модели
	var structured *models.StructuredData
	if resp.Structured != nil {
		structured = &models.StructuredData{
			State:       resp.Structured.State,
			StateName:   resp.Structured.StateName,
			Countdown:   resp.Structured.Countdown,
			Progress:    resp.Structured.Progress,
			Cycle:       resp.Structured.Cycle,
			TotalCycles: resp.Structured.TotalCycles,
			Status:      resp.Structured.Status,
			Completed:   resp.Structured.Completed,
			Message:     resp.Structured.Message,
			Step:        resp.Structured.Step,
			StepName:    resp.Structured.StepName,
		}
	}

	feedback := &models.FrameFeedback{
		FistDetected:    resp.FistDetected,
		HandDetected:    resp.HandDetected,
		RaisedFingers:   resp.RaisedFingers,
		FingerStates:    resp.FingerStates,
		Message:         resp.Message,
		ProcessedFrame:  resp.ProcessedFrame,
		CurrentExercise: resp.CurrentExercise,
		ExerciseName:    resp.ExerciseName,
		Structured:      structured,
		Timestamp:       time.Now().Unix(),
	}

	log.Printf("📥 Ответ от Python на сброс: status=%s, message='%s'", resp.Status, resp.Message)

	return feedback, nil
}

func (h *ExerciseHandler) writePump(client *websocket.Client) {
	ticker := time.NewTicker(30 * time.Second)
	defer func() {
		log.Printf("writePump exiting for client: %s", client.ExerciseID)
		ticker.Stop()
		err := client.Conn.Close()
		if err != nil {
			return
		}
	}()

	for {
		select {
		case message, ok := <-client.Send:
			if !ok {
				err := client.Conn.WriteMessage(gorilla.CloseMessage, []byte{})
				if err != nil {
					return
				}
				return
			}

			err := client.Conn.SetWriteDeadline(time.Now().Add(30 * time.Second))
			if err != nil {
				return
			}
			if err := client.Conn.WriteMessage(gorilla.TextMessage, message); err != nil {
				log.Printf("writePump error for %s: %v", client.ExerciseID, err)
				return
			}

		case <-ticker.C:
			err := client.Conn.SetWriteDeadline(time.Now().Add(30 * time.Second))
			if err != nil {
				return
			}
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				log.Printf("Ping error for %s: %v", client.ExerciseID, err)
				return
			}
		}
	}
}

func (h *ExerciseHandler) processFrame(messageStr string) (*models.FrameFeedback, error) {
	log.Printf("📤 Отправка в Python, размер данных: %d байт", len(messageStr))

	var clientMsg map[string]interface{}
	if err := json.Unmarshal([]byte(messageStr), &clientMsg); err != nil {
		log.Printf("❌ Ошибка парсинга сообщения клиента: %v", err)
		return nil, err
	}

	frameData, ok := clientMsg["frame"].(string)
	if !ok {
		log.Printf("❌ Нет поля frame в сообщении")
		return nil, fmt.Errorf("no frame data")
	}

	pythonRequest := map[string]interface{}{
		"frame": frameData,
	}

	if exType, ok := clientMsg["exercise_type"]; ok {
		pythonRequest["exercise_type"] = exType
	}

	resp, err := h.pythonClient.ProcessFrame(pythonRequest)
	if err != nil {
		log.Printf("❌ Ошибка Python: %v", err)
		return nil, err
	}

	// Конвертируем StructuredData для модели
	var structured *models.StructuredData
	if resp.Structured != nil {
		structured = &models.StructuredData{
			State:       resp.Structured.State,
			StateName:   resp.Structured.StateName,
			Countdown:   resp.Structured.Countdown,
			Progress:    resp.Structured.Progress,
			Cycle:       resp.Structured.Cycle,
			TotalCycles: resp.Structured.TotalCycles,
			Status:      resp.Structured.Status,
			Completed:   resp.Structured.Completed,
			Message:     resp.Structured.Message,
			Step:        resp.Structured.Step,
			StepName:    resp.Structured.StepName,
		}
		log.Printf("📊 Структурированные данные: состояние=%v, цикл=%d/%d, завершено=%v",
			resp.Structured.State,
			resp.Structured.Cycle,
			resp.Structured.TotalCycles,
			resp.Structured.Completed)
	}

	feedback := &models.FrameFeedback{
		FistDetected:    resp.FistDetected,
		HandDetected:    resp.HandDetected,
		RaisedFingers:   resp.RaisedFingers,
		FingerStates:    resp.FingerStates,
		Message:         resp.Message,
		ProcessedFrame:  resp.ProcessedFrame,
		CurrentExercise: resp.CurrentExercise,
		ExerciseName:    resp.ExerciseName,
		Structured:      structured,
		Timestamp:       time.Now().Unix(),
	}

	log.Printf("📥 Ответ от Python: status=%s, hand_detected=%v, message='%s', structured=%v",
		resp.Status, resp.HandDetected, resp.Message, resp.Structured != nil)

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
