// internal/websocket/cluster_hub.go
package websocket

import (
	"context"
	"encoding/json"
	"fmt"
	"lfk-backend/internal/config"
	"lfk-backend/internal/redis"
	"log"
	"sync"
	"time"
)

type ClusterHub struct {
	// Локальные клиенты на этом сервере
	clients    map[*Client]bool
	register   chan *Client
	unregister chan *Client
	broadcast  chan []byte
	mu         sync.RWMutex

	// Redis клиент для межсерверной коммуникации
	redisClient *redis.RedisClient
	pubsub      *redis.PubSubManager

	// Информация о сервере
	serverID   string
	serverAddr string
	startTime  time.Time

	// Конфигурация
	config *config.WebSocketConfig
}

type ClusterMessage struct {
	Type       string          `json:"type"` // broadcast, direct, server_event
	ServerID   string          `json:"server_id"`
	UserID     string          `json:"user_id,omitempty"`
	ExerciseID string          `json:"exercise_id,omitempty"`
	Data       json.RawMessage `json:"data"`
	Timestamp  int64           `json:"timestamp"`
}

type ServerInfo struct {
	ID           string         `json:"id"`
	Address      string         `json:"address"`
	Connections  int            `json:"connections"`
	StartTime    time.Time      `json:"start_time"`
	LastSeen     time.Time      `json:"last_seen"`
	ExerciseLoad map[string]int `json:"exercise_load"`
}

func NewClusterHub(redisClient *redis.RedisClient, serverID, serverAddr string, cfg *config.WebSocketConfig) *ClusterHub {
	pubsub := redis.NewPubSubManager(redisClient)

	hub := &ClusterHub{
		clients:     make(map[*Client]bool),
		register:    make(chan *Client),
		unregister:  make(chan *Client),
		broadcast:   make(chan []byte, 256),
		redisClient: redisClient,
		pubsub:      pubsub,
		serverID:    serverID,
		serverAddr:  serverAddr,
		startTime:   time.Now(),
		config:      cfg,
	}

	return hub
}

func (h *ClusterHub) Run() {
	// Подписываемся на глобальные события Redis
	h.subscribeToRedis()

	// Запускаем heartbeat для регистрации в кластере
	go h.heartbeat()

	// Запускаем сборщик метрик
	go h.collectMetrics()

	for {
		select {
		case client := <-h.register:
			h.registerClient(client)

		case client := <-h.unregister:
			h.unregisterClient(client)

		case message := <-h.broadcast:
			h.broadcastLocal(message)
		}
	}
}

func (h *ClusterHub) subscribeToRedis() {
	handlers := map[string]redis.MessageHandler{
		"cluster:broadcast":            h.handleClusterBroadcast,
		"cluster:direct:" + h.serverID: h.handleClusterDirect,
		"cluster:events":               h.handleClusterEvent,
	}

	if err := h.pubsub.Subscribe(handlers); err != nil {
		log.Printf("Failed to subscribe to Redis channels: %v", err)
	}
}

func (h *ClusterHub) heartbeat() {
	ticker := time.NewTicker(5 * time.Second)
	defer ticker.Stop()

	ctx := context.Background()

	for range ticker.C {
		h.mu.RLock()
		connCount := len(h.clients)

		// Собираем статистику по упражнениям
		exerciseLoad := make(map[string]int)
		for client := range h.clients {
			if client.ExerciseID != "" {
				exerciseLoad[client.ExerciseID]++
			}
		}
		h.mu.RUnlock()

		serverInfo := ServerInfo{
			ID:           h.serverID,
			Address:      h.serverAddr,
			Connections:  connCount,
			StartTime:    h.startTime,
			LastSeen:     time.Now(),
			ExerciseLoad: exerciseLoad,
		}

		data, _ := json.Marshal(serverInfo)

		// Сохраняем информацию о сервере в Redis
		h.redisClient.Set(ctx, "server:"+h.serverID, data, 10*time.Second)

		// Публикуем событие о сервере
		h.publishServerEvent("heartbeat", serverInfo)
	}
}

func (h *ClusterHub) collectMetrics() {
	ticker := time.NewTicker(1 * time.Minute)
	defer ticker.Stop()

	ctx := context.Background()

	for range ticker.C {
		h.mu.RLock()
		connCount := len(h.clients)
		h.mu.RUnlock()

		// Обновляем метрики в Redis
		h.redisClient.Set(ctx, "metrics:"+h.serverID+":connections", connCount, 2*time.Minute)
	}
}

func (h *ClusterHub) registerClient(client *Client) {
	h.mu.Lock()
	h.clients[client] = true
	h.mu.Unlock()

	log.Printf("Client registered on server %s: %s (total: %d)",
		h.serverID, client.ExerciseID, len(h.clients))

	// Публикуем событие о новом клиенте
	h.publishClientEvent("client_connected", client)
}

func (h *ClusterHub) unregisterClient(client *Client) {
	h.mu.Lock()
	if _, ok := h.clients[client]; ok {
		delete(h.clients, client)
		close(client.Send)
	}
	h.mu.Unlock()

	log.Printf("Client unregistered on server %s: %s (total: %d)",
		h.serverID, client.ExerciseID, len(h.clients))

	// Публикуем событие об отключении клиента
	h.publishClientEvent("client_disconnected", client)
}

func (h *ClusterHub) broadcastLocal(message []byte) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	for client := range h.clients {
		select {
		case client.Send <- message:
		default:
			// Клиент не успевает читать, закрываем соединение
			h.mu.RUnlock()
			h.unregister <- client
			h.mu.RLock()
		}
	}
}

// Broadcast отправляет сообщение всем клиентам во всем кластере
func (h *ClusterHub) Broadcast(message []byte) {
	// Отправляем локально
	h.broadcast <- message

	// Отправляем в кластер
	clusterMsg := ClusterMessage{
		Type:      "broadcast",
		ServerID:  h.serverID,
		Data:      message,
		Timestamp: time.Now().Unix(),
	}

	h.publishClusterMessage("cluster:broadcast", clusterMsg)
}

// SendToUser отправляет сообщение конкретному пользователю в кластере
func (h *ClusterHub) SendToUser(userID string, message []byte) error {
	// Проверяем локально
	h.mu.RLock()
	for client := range h.clients {
		if client.UserID == userID {
			h.mu.RUnlock()
			select {
			case client.Send <- message:
				return nil
			default:
				return fmt.Errorf("client send buffer full")
			}
		}
	}
	h.mu.RUnlock()

	// Если не нашли локально, отправляем в кластер
	clusterMsg := ClusterMessage{
		Type:      "direct",
		ServerID:  h.serverID,
		UserID:    userID,
		Data:      message,
		Timestamp: time.Now().Unix(),
	}

	return h.publishClusterMessage("cluster:direct:"+userID, clusterMsg)
}

// GetStats возвращает статистику хаба
func (h *ClusterHub) GetStats() map[string]interface{} {
	h.mu.RLock()
	defer h.mu.RUnlock()

	exerciseStats := make(map[string]int)
	for client := range h.clients {
		exerciseStats[client.ExerciseID]++
	}

	return map[string]interface{}{
		"server_id":         h.serverID,
		"total_connections": len(h.clients),
		"exercise_stats":    exerciseStats,
		"uptime":            time.Since(h.startTime).Seconds(),
	}
}

func (h *ClusterHub) handleClusterBroadcast(channel string, data []byte) {
	var msg ClusterMessage
	if err := json.Unmarshal(data, &msg); err != nil {
		log.Printf("Failed to unmarshal cluster message: %v", err)
		return
	}

	// Не обрабатываем свои сообщения
	if msg.ServerID == h.serverID {
		return
	}

	h.broadcastLocal(msg.Data)
}

func (h *ClusterHub) handleClusterDirect(channel string, data []byte) {
	var msg ClusterMessage
	if err := json.Unmarshal(data, &msg); err != nil {
		log.Printf("Failed to unmarshal cluster message: %v", err)
		return
	}

	// Ищем пользователя локально
	h.mu.RLock()
	defer h.mu.RUnlock()

	for client := range h.clients {
		if client.UserID == msg.UserID {
			select {
			case client.Send <- msg.Data:
			default:
				log.Printf("Failed to send direct message to user %s: buffer full", msg.UserID)
			}
			return
		}
	}
}

func (h *ClusterHub) handleClusterEvent(channel string, data []byte) {
	var event struct {
		Type     string          `json:"type"`
		ServerID string          `json:"server_id"`
		Data     json.RawMessage `json:"data"`
	}

	if err := json.Unmarshal(data, &event); err != nil {
		log.Printf("Failed to unmarshal cluster event: %v", err)
		return
	}

	switch event.Type {
	case "heartbeat":
		var info ServerInfo
		if err := json.Unmarshal(event.Data, &info); err == nil {
			log.Printf("Server %s heartbeat: %d connections", info.ID, info.Connections)
		}
	case "client_connected", "client_disconnected":
		log.Printf("Event %s from server %s", event.Type, event.ServerID)
	}
}

func (h *ClusterHub) publishClusterMessage(channel string, msg interface{}) error {
	return h.pubsub.PublishJSON(channel, msg)
}

func (h *ClusterHub) publishServerEvent(eventType string, data interface{}) {
	event := struct {
		Type     string      `json:"type"`
		ServerID string      `json:"server_id"`
		Data     interface{} `json:"data"`
	}{
		Type:     eventType,
		ServerID: h.serverID,
		Data:     data,
	}

	h.publishClusterMessage("cluster:events", event)
}

func (h *ClusterHub) publishClientEvent(eventType string, client *Client) {
	event := struct {
		Type       string `json:"type"`
		ServerID   string `json:"server_id"`
		UserID     string `json:"user_id"`
		ExerciseID string `json:"exercise_id"`
	}{
		Type:       eventType,
		ServerID:   h.serverID,
		UserID:     client.UserID,
		ExerciseID: client.ExerciseID,
	}

	h.publishClusterMessage("cluster:events", event)
}

// internal/websocket/cluster_hub.go - добавьте эти методы

// RegisterClient регистрирует нового клиента
func (h *ClusterHub) RegisterClient(client *Client) {
	if h.register != nil {
		h.register <- client
	} else {
		log.Printf("⚠️ ClusterHub register channel is nil")
	}
}

// UnregisterClient удаляет клиента из хаба
func (h *ClusterHub) UnregisterClient(client *Client) {
	if h.unregister != nil {
		h.unregister <- client
	} else {
		log.Printf("⚠️ ClusterHub unregister channel is nil")
	}
}
