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
	"github.com/jmoiron/sqlx"
)

func main() {
	// Загружаем конфигурацию
	cfg := config.LoadConfig()

	// Подключаемся к БД
	db := config.InitDB(cfg)
	defer func(db *sqlx.DB) {
		err := db.Close()
		if err != nil {

		}
	}(db)

	// Инициализируем JWT менеджер
	jwtManager := auth.NewJWTManager(cfg.JWTSecret, 24*time.Hour)

	// Инициализация роутера
	router := gin.Default()

	// Инициализация WebSocket хаба
	hub := websocket.NewHub()
	go hub.Run()

	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)
	exerciseRepo := repository.NewExerciseRepository(db)
	workoutRepo := repository.NewWorkoutRepository(db)
	statsRepo := repository.NewStatsRepository(db)

	// Создаем обработчики
	exerciseHandler := handlers.NewExerciseHandler(hub, "http://localhost:5001")
	userHandler := handlers.NewUserHandler(userRepo, jwtManager)
	workoutHandler := handlers.NewWorkoutHandler(workoutRepo, statsRepo, exerciseRepo)
	statsHandler := handlers.NewStatsHandler(statsRepo)

	// Публичные маршруты
	public := router.Group("/api")
	{
		public.POST("/register", userHandler.Register)
		public.POST("/login", userHandler.Login)
		public.GET("/health", func(c *gin.Context) {
			c.JSON(200, gin.H{"status": "ok"})
		})
		public.GET("/user/check", userHandler.CheckUser)
		public.GET("/user/check/email", userHandler.CheckEmail)
		public.GET("/user/check/username", userHandler.CheckUsername)
	}

	// Защищенные маршруты
	protected := router.Group("/api")
	protected.Use(middleware.AuthMiddleware(jwtManager))
	{
		// Профиль
		protected.GET("/profile", userHandler.GetProfile)
		protected.PUT("/profile", userHandler.UpdateProfile)
		protected.POST("/change-password", userHandler.ChangePassword)

		// Упражнения
		protected.GET("/exercises", workoutHandler.GetExercises)
		protected.GET("/exercises/:id", workoutHandler.GetExercise)

		// Тренировки
		protected.POST("/workout/start", workoutHandler.StartWorkout)
		protected.POST("/workout/end", workoutHandler.EndWorkout)
		protected.POST("/workout/exercise", workoutHandler.AddExerciseSet)
		protected.GET("/workout/history", workoutHandler.GetWorkoutHistory)
		protected.GET("/workout/current", workoutHandler.GetCurrentWorkout)
		protected.GET("/workout/session/:id", workoutHandler.GetSessionDetails)

		// Статистика
		protected.GET("/stats/overall", statsHandler.GetOverallStats)
		protected.GET("/stats/daily", statsHandler.GetDailyStats)
		protected.GET("/stats/weekly", statsHandler.GetWeeklyStats)
		protected.GET("/stats/monthly", statsHandler.GetMonthlyStats)
		protected.GET("/stats/exercises", statsHandler.GetExerciseStats)
		protected.GET("/stats/exercises/:id", statsHandler.GetExerciseStatsByID)
		protected.GET("/stats/period", statsHandler.GetStatsForPeriod)
		protected.GET("/stats/history", statsHandler.GetWorkoutHistory)

		// Дашборд
		protected.GET("/dashboard", statsHandler.GetDashboard)
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
