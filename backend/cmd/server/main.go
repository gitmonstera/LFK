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

	// Создаем обработчик с URL Python сервера
	exerciseHandler := handlers.NewExerciseHandler(hub, "http://localhost:5001")

	// Маршруты API
	api := router.Group("/api")
	{
		api.GET("/health", func(c *gin.Context) {
			c.JSON(200, gin.H{"status": "ok"})
		})

		// Отдельные WebSocket эндпоинты для каждого упражнения
		router.GET("/ws/exercise/fist", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "fist")
		})

		router.GET("/ws/exercise/fist-index", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "fist-index")
		})

		// Можно добавить другие упражнения
		router.GET("/ws/exercise/palm", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "palm")
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
