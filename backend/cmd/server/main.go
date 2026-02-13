package main

import (
	"lfk-backend/internal/handlers"
	"lfk-backend/internal/websocket"
	"log"

	"github.com/gin-gonic/gin"
)

func main() {
	// Инициализация роутера
	router := gin.Default()

	// Инициализация WebSocket хаба
	hub := websocket.NewHub()
	go hub.Run()

	// Инициализация Python бриджа
	pythonBridge := handlers.NewPythonBridge("http://localhost:5001")

	// Создаем обработчики
	exerciseHandler := handlers.NewExerciseHandler(hub, pythonBridge)

	// Маршруты API
	api := router.Group("/api")
	{
		api.GET("/health", func(c *gin.Context) {
			c.JSON(200, gin.H{"status": "ok"})
		})

		// WebSocket для упражнений
		router.GET("/ws/exercise/:exerciseId", func(c *gin.Context) {
			exerciseId := c.Param("exerciseId")
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, exerciseId)
		})

		// REST эндпоинты
		api.POST("/exercise/start", exerciseHandler.StartExercise)
		api.POST("/exercise/stop", exerciseHandler.StopExercise)
	}

	log.Println("Server starting on :8080")
	if err := router.Run(":8080"); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
