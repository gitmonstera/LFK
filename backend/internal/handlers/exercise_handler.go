// internal/handlers/exercise_handler.go
package handlers

import (
	"context"
	"encoding/json"
	"fmt"
	"lfk-backend/internal/config"
	"lfk-backend/internal/models"
	_ "lfk-backend/internal/models"
	"lfk-backend/internal/redis"
	"lfk-backend/internal/repository"
	"lfk-backend/internal/websocket"
	"lfk-backend/pkg/python_bridge"
	"log"
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin" // Добавьте этот импорт
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
	hub          *websocket.ClusterHub
	pythonClient *python_bridge.Client
	redisClient  *redis.RedisClient
	exerciseRepo *repository.ExerciseRepository
	sessions     sync.Map
	config       *config.Config
	serverID     string

	// Кэш
	stateCache *sync.Map
}

type SessionInfo struct {
	ID          string    `json:"id"`
	UserID      string    `json:"user_id"`
	ExerciseID  string    `json:"exercise_id"`
	StartTime   time.Time `json:"start_time"`
	LastFrameAt time.Time `json:"last_frame_at"`
	FrameCount  int64     `json:"frame_count"`
	ServerID    string    `json:"server_id"`
}

func NewExerciseHandler(
	hub *websocket.ClusterHub,
	pythonClient *python_bridge.Client,
	redisClient *redis.RedisClient,
	exerciseRepo *repository.ExerciseRepository,
	cfg *config.Config,
	serverID string,
) *ExerciseHandler {
	return &ExerciseHandler{
		hub:          hub,
		pythonClient: pythonClient,
		redisClient:  redisClient,
		exerciseRepo: exerciseRepo,
		config:       cfg,
		serverID:     serverID,
		stateCache:   &sync.Map{},
	}
}

// HandleWebSocket - обработка WebSocket соединений
func (h *ExerciseHandler) HandleWebSocket(w http.ResponseWriter, r *http.Request, exerciseId string) {
	// Получаем user_id из контекста запроса
	userIDValue := r.Context().Value("user_id")

	if userIDValue == nil {
		log.Printf("❌ user_id not found in context")
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	userID, ok := userIDValue.(string)
	if !ok {
		log.Printf("❌ user_id is not string, got: %T", userIDValue)
		http.Error(w, "Unauthorized", http.StatusUnauthorized)
		return
	}

	log.Printf("🔌 WebSocket connection from user %s for exercise %s", userID, exerciseId)

	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("Failed to upgrade connection: %v", err)
		return
	}

	// Настраиваем таймауты
	conn.SetReadLimit(h.config.WebSocket.MaxMessageSize)
	conn.SetReadDeadline(time.Now().Add(h.config.WebSocket.ReadTimeout))
	conn.SetWriteDeadline(time.Now().Add(h.config.WebSocket.WriteTimeout))

	conn.SetPongHandler(func(string) error {
		conn.SetReadDeadline(time.Now().Add(h.config.WebSocket.ReadTimeout))
		return nil
	})

	// Создаем клиента
	client := &websocket.Client{
		Conn:       conn,
		Send:       make(chan []byte, 256),
		ExerciseID: exerciseId,
		UserID:     userID,
	}

	// Регистрируем клиента
	if h.hub != nil {
		h.hub.RegisterClient(client)
	} else {
		log.Printf("⚠️ Hub is nil, client not registered")
	}

	// Создаем сессию
	sessionID := uuid.New().String()
	session := &SessionInfo{
		ID:          sessionID,
		UserID:      userID,
		ExerciseID:  exerciseId,
		StartTime:   time.Now(),
		LastFrameAt: time.Now(),
		ServerID:    h.serverID,
	}
	h.sessions.Store(sessionID, session)

	// Сохраняем сессию в Redis
	ctx := context.Background()
	sessionData, _ := json.Marshal(session)
	h.redisClient.Set(ctx, "session:"+sessionID, sessionData, 1*time.Hour)
	h.redisClient.Set(ctx, "user:"+userID+":session", sessionID, 1*time.Hour)

	// Запускаем горутины
	go h.readPump(client, sessionID)
	go h.writePump(client)
}

// readPump - чтение сообщений от клиента
func (h *ExerciseHandler) readPump(client *websocket.Client, sessionID string) {
	defer func() {
		log.Printf("readPump exiting for user %s", client.UserID)
		h.hub.UnregisterClient(client)
		client.Conn.Close()
		h.sessions.Delete(sessionID)
		h.redisClient.Del(context.Background(), "session:"+sessionID)
	}()

	for {
		_, message, err := client.Conn.ReadMessage()
		if err != nil {
			if gorilla.IsUnexpectedCloseError(err, gorilla.CloseGoingAway, gorilla.CloseAbnormalClosure) {
				log.Printf("readPump error for user %s: %v", client.UserID, err)
			}
			break
		}

		// Обновляем сессию
		if session, ok := h.getSession(sessionID); ok {
			session.LastFrameAt = time.Now()
			session.FrameCount++
			h.sessions.Store(sessionID, session)
		}

		// Обрабатываем сообщение
		go h.processClientMessage(client, sessionID, message)
	}
}

// processClientMessage - обработка сообщения от клиента
func (h *ExerciseHandler) processClientMessage(client *websocket.Client, sessionID string, message []byte) {
	log.Printf("📥 Received message from user %s, size: %d bytes", client.UserID, len(message))

	var clientMsg map[string]interface{}
	if err := json.Unmarshal(message, &clientMsg); err != nil {
		log.Printf("❌ Error parsing client message: %v", err)
		return
	}

	ctx := context.Background()

	// Проверяем запрос на сброс
	if reset, ok := clientMsg["reset_for_new_attempt"]; ok && reset == true {
		log.Printf("🔄 Reset request from user %s", client.UserID)
		h.handleReset(client, clientMsg)
		return
	}

	// Получаем кадр
	frameData, ok := clientMsg["frame"].(string)
	if !ok {
		log.Printf("❌ No frame data in message from user %s", client.UserID)
		return
	}
	log.Printf("📸 Frame received from user %s, base64 length: %d", client.UserID, len(frameData))

	exerciseType := client.ExerciseID
	if et, ok := clientMsg["exercise_type"]; ok {
		exerciseType = et.(string)
	}
	log.Printf("🏋️ Exercise type: %s", exerciseType)

	// ВАЖНО: Всегда отправляем кадры в Python, кроме случаев когда это явно запрос состояния
	// Проверяем, есть ли флаг, что это запрос только состояния
	if _, isStateOnly := clientMsg["get_state_only"]; isStateOnly {
		// Проверяем кэш состояния только для явных запросов состояния
		cacheKey := fmt.Sprintf("exercise_state:%s:%s", client.UserID, exerciseType)
		if cached, err := h.redisClient.Get(ctx, cacheKey); err == nil && cached != "" {
			log.Printf("✅ Sending cached state to user %s", client.UserID)
			client.Send <- []byte(cached)
			return
		}
	}

	// Отправляем в Python синхронно (ВСЕГДА для кадров)
	log.Printf("📤 Sending frame to Python processor for user %s", client.UserID)
	pythonRequest := map[string]interface{}{
		"frame":         frameData,
		"exercise_type": exerciseType,
	}

	resp, err := h.pythonClient.ProcessFrame(ctx, pythonRequest)
	if err != nil {
		log.Printf("❌ Failed to process frame: %v", err)
		errorMsg := map[string]interface{}{
			"status":  "error",
			"message": "Failed to process frame",
		}
		errorJSON, _ := json.Marshal(errorMsg)
		client.Send <- errorJSON
		return
	}

	log.Printf("✅ Received response from Python processor for user %s, status: %s",
		client.UserID, resp.Status)

	// Кэшируем результат ТОЛЬКО если это не ошибка
	if resp != nil && resp.Status == "success" && resp.Structured != nil {
		log.Printf("📊 Exercise state for user %s: cycle=%d, completed=%v",
			client.UserID, resp.Structured.Cycle, resp.Structured.Completed)
		cacheKey := fmt.Sprintf("exercise_state:%s:%s", client.UserID, exerciseType)
		cacheData, _ := json.Marshal(resp)
		h.redisClient.Set(ctx, cacheKey, cacheData, h.config.Cache.ExerciseStateTTL)
	}

	// Отправляем результат клиенту
	feedbackJSON, _ := json.Marshal(resp)
	client.Send <- feedbackJSON
	log.Printf("📤 Sent feedback to user %s, size: %d bytes", client.UserID, len(feedbackJSON))

	// Если упражнение завершено, сохраняем статистику
	if resp != nil && resp.Structured != nil && resp.Structured.Completed {
		log.Printf("🎯 Exercise completed for user %s", client.UserID)
		go h.saveExerciseStats(client.UserID, sessionID, exerciseType, resp)
	}
}

// handleReset - обработка сброса упражнения
func (h *ExerciseHandler) handleReset(client *websocket.Client, clientMsg map[string]interface{}) {
	log.Printf("🔄 Processing reset for user %s", client.UserID)

	ctx := context.Background()

	// Очищаем кэш
	cacheKey := fmt.Sprintf("exercise_state:%s:%s", client.UserID, client.ExerciseID)
	h.redisClient.Del(ctx, cacheKey)

	// Отправляем запрос в Python
	resetRequest := map[string]interface{}{
		"exercise_type":         client.ExerciseID,
		"reset_for_new_attempt": true,
	}

	resp, err := h.pythonClient.ProcessFrame(ctx, resetRequest)
	if err != nil {
		log.Printf("Failed to reset exercise: %v", err)
		return
	}

	feedbackJSON, _ := json.Marshal(resp)
	client.Send <- feedbackJSON
}

// writePump - отправка сообщений клиенту
func (h *ExerciseHandler) writePump(client *websocket.Client) {
	ticker := time.NewTicker(h.config.WebSocket.PingInterval)
	defer func() {
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

			client.Conn.SetWriteDeadline(time.Now().Add(h.config.WebSocket.WriteTimeout))
			if err := client.Conn.WriteMessage(gorilla.TextMessage, message); err != nil {
				log.Printf("writePump error for user %s: %v", client.UserID, err)
				return
			}

		case <-ticker.C:
			client.Conn.SetWriteDeadline(time.Now().Add(h.config.WebSocket.WriteTimeout))
			if err := client.Conn.WriteMessage(gorilla.PingMessage, nil); err != nil {
				return
			}
		}
	}
}

// subscribeToUserResults - подписка на результаты обработки
func (h *ExerciseHandler) subscribeToUserResults(userID string, client *websocket.Client) {
	// Упрощенная версия без pubsub для начала
}

// getSession - получение сессии
func (h *ExerciseHandler) getSession(sessionID string) (*SessionInfo, bool) {
	if val, ok := h.sessions.Load(sessionID); ok {
		return val.(*SessionInfo), true
	}
	return nil, false
}

// saveExerciseStats - сохранение статистики упражнения
func (h *ExerciseHandler) saveExerciseStats(userID, sessionID, exerciseType string, feedback *python_bridge.FrameResponse) {
	log.Printf("Saving stats for user %s, exercise %s", userID, exerciseType)
}

// GetExerciseState - получение состояния упражнения
func (h *ExerciseHandler) GetExerciseState(c *gin.Context) {
	exerciseType := c.Query("type")
	if exerciseType == "" {
		exerciseType = "fist-palm"
	}

	userID := c.GetString("user_id")
	log.Printf("📊 Exercise state request for user %s, exercise %s", userID, exerciseType)

	ctx := context.Background()

	// Проверяем кэш
	cacheKey := fmt.Sprintf("exercise_state:%s:%s", userID, exerciseType)
	if cached, err := h.redisClient.Get(ctx, cacheKey); err == nil && cached != "" {
		c.Data(http.StatusOK, "application/json", []byte(cached))
		return
	}

	// Запрашиваем из Python
	stateRequest := map[string]interface{}{
		"exercise_type":  exerciseType,
		"get_state_only": true,
	}

	resp, err := h.pythonClient.ProcessFrame(ctx, stateRequest)
	if err != nil {
		log.Printf("Failed to get exercise state: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to get exercise state",
		})
		return
	}

	// Кэшируем
	respJSON, _ := json.Marshal(resp)
	h.redisClient.Set(ctx, cacheKey, respJSON, h.config.Cache.ExerciseStateTTL)

	c.JSON(http.StatusOK, resp)
}

// ResetExercise - сброс упражнения
func (h *ExerciseHandler) ResetExercise(c *gin.Context) {
	var req struct {
		ExerciseType string `json:"exercise_type" binding:"required"`
	}

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	userID := c.GetString("user_id")
	log.Printf("🔄 Reset exercise for user %s: %s", userID, req.ExerciseType)

	ctx := context.Background()

	// Очищаем кэш
	cacheKey := fmt.Sprintf("exercise_state:%s:%s", userID, req.ExerciseType)
	h.redisClient.Del(ctx, cacheKey)

	// Отправляем в Python
	resetRequest := map[string]interface{}{
		"exercise_type":         req.ExerciseType,
		"reset_for_new_attempt": true,
	}

	resp, err := h.pythonClient.ProcessFrame(ctx, resetRequest)
	if err != nil {
		log.Printf("Failed to reset exercise: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to reset exercise"})
		return
	}

	c.JSON(http.StatusOK, resp)
}

// GetExerciseListFromDB - список упражнений из БД
func (h *ExerciseHandler) GetExerciseListFromDB(c *gin.Context) {
	log.Printf("📋 Fetching exercise list from database")

	// Получаем список упражнений из репозитория
	exercises, err := h.exerciseRepo.GetExerciseList()
	if err != nil {
		log.Printf("❌ Failed to get exercise list: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":   "Failed to get exercise list",
			"details": err.Error(),
		})
		return
	}

	// Если упражнений нет, возвращаем пустой массив
	if exercises == nil {
		exercises = []models.ExerciseInfo{}
	}

	// Формируем ответ в нужном формате
	response := models.ExerciseListResponse{
		Items: exercises,
	}

	log.Printf("✅ Returning %d exercises", len(exercises))
	c.JSON(http.StatusOK, response)
}
