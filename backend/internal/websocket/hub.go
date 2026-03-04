// internal/websocket/hub.go
package websocket

import (
	_ "sync"

	gorilla "github.com/gorilla/websocket"
)

type Client struct {
	Hub        *ClusterHub
	Conn       *gorilla.Conn
	Send       chan []byte
	ExerciseID string
	UserID     string
}

type Hub struct {
	// Для обратной совместимости, но используем ClusterHub внутри
	clusterHub *ClusterHub
}

func NewHub() *Hub {
	return &Hub{}
}

// SetClusterHub устанавливает кластерный хаб
func (h *Hub) SetClusterHub(clusterHub *ClusterHub) {
	h.clusterHub = clusterHub
}

func (h *Hub) Run() {
	// Делегируем кластерному хабу
	if h.clusterHub != nil {
		h.clusterHub.Run()
	}
}
