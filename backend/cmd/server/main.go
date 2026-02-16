package main

import (
	"lfk-backend/config"
	"lfk-backend/internal/auth"
	"lfk-backend/internal/handlers"
	"lfk-backend/internal/middleware"
	"lfk-backend/internal/repository"
	"lfk-backend/internal/websocket"
	"log"
	"time"

	"github.com/gin-gonic/gin"
)

func main() {
	// Загружаем конфигурацию
	cfg := config.LoadConfig()

	// Подключаемся к БД
	db := config.InitDB(cfg)
	defer db.Close()

	// Инициализируем JWT менеджер
	jwtManager := auth.NewJWTManager(cfg.JWTSecret, 24*time.Hour)

	// Инициализация роутера
	router := gin.Default()

	// Инициализация WebSocket хаба
	hub := websocket.NewHub()
	go hub.Run()

	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)

	// Создаем обработчики
	exerciseHandler := handlers.NewExerciseHandler(hub, "http://localhost:5001")
	userHandler := handlers.NewUserHandler(userRepo, jwtManager)

	// ПУБЛИЧНЫЕ МАРШРУТЫ (не требуют аутентификации)
	public := router.Group("/api")
	{
		public.POST("/register", userHandler.Register)
		public.POST("/login", userHandler.Login)
		public.GET("/health", func(c *gin.Context) {
			c.JSON(200, gin.H{"status": "ok"})
		})

		// Маршруты для проверки пользователей (тоже публичные)
		public.GET("/user/check", userHandler.CheckUser)
		public.GET("/user/check/email", userHandler.CheckEmail)
		public.GET("/user/check/username", userHandler.CheckUsername)
	}

	// ЗАЩИЩЕННЫЕ МАРШРУТЫ (требуют JWT токен)
	protected := router.Group("/api")
	protected.Use(middleware.AuthMiddleware(jwtManager))
	{
		// Профиль пользователя
		protected.GET("/profile", userHandler.GetProfile)

		// Упражнения (REST)
		protected.POST("/exercise/start", exerciseHandler.StartExercise)
		protected.POST("/exercise/stop", exerciseHandler.StopExercise)
	}

	// WebSocket маршруты (тоже защищенные)
	ws := router.Group("/ws")
	ws.Use(middleware.AuthMiddleware(jwtManager))
	{
		ws.GET("/exercise/fist", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "fist")
		})
		ws.GET("/exercise/fist-index", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "fist-index")
		})
		ws.GET("/exercise/fist-palm", func(c *gin.Context) {
			exerciseHandler.HandleWebSocket(c.Writer, c.Request, "fist-palm")
		})
	}

	log.Println("Server starting on :8080")
	if err := router.Run(":8080"); err != nil {
		log.Fatal("Failed to start server:", err)
	}
}
