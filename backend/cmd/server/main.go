// cmd/main.go - исправленный импорт и инициализация

package main

import (
	"context"
	"fmt"
	"lfk-backend/internal/config"
	"lfk-backend/internal/handlers"
	"lfk-backend/internal/middleware"
	"lfk-backend/internal/redis"
	"lfk-backend/internal/repository"
	"lfk-backend/internal/websocket"
	"lfk-backend/pkg/python_bridge"
	"log"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"strings"
	"syscall"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

func main() {
	// Загружаем конфигурацию
	cfg, err := config.LoadConfig()
	if err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	// Генерируем уникальный ID для этого сервера
	hostname, _ := os.Hostname()
	serverID := fmt.Sprintf("%s-%d", hostname, os.Getpid())
	log.Printf("Starting server with ID: %s", serverID)

	// Подключаемся к базе данных
	db := initDatabase(cfg)

	// Подключаемся к Redis
	redisClient, err := redis.NewRedisClient(&cfg.Redis, serverID)
	if err != nil {
		log.Fatalf("Failed to connect to Redis: %v", err)
	}
	defer redisClient.Close()

	// Создаем Python клиент с поддержкой пула и очередей
	pythonClient, err := python_bridge.NewClient(redisClient, &cfg.PythonProcessor)
	if err != nil {
		log.Fatalf("Failed to create Python client: %v", err)
	}

	// Создаем кластерный WebSocket хаб
	hub := websocket.NewClusterHub(redisClient, serverID,
		fmt.Sprintf("%s:%d", hostname, cfg.Server.HTTPPort),
		&cfg.WebSocket)
	go hub.Run()

	// Инициализируем репозитории
	userRepo := repository.NewUserRepository(db)
	exerciseRepo := repository.NewExerciseRepository(db)
	workoutRepo := repository.NewWorkoutRepository(db)
	statsRepo := repository.NewStatsRepository(db)

	// Создаем обработчики
	exerciseHandler := handlers.NewExerciseHandler(
		hub,
		pythonClient,
		redisClient,
		exerciseRepo,
		cfg,
		serverID,
	)

	// Исправляем создание userHandler - передаем правильные параметры
	userHandler := handlers.NewUserHandler(userRepo, cfg.Auth.JWTSecret, cfg.Auth.TokenDuration)

	workoutHandler := handlers.NewWorkoutHandler(workoutRepo, statsRepo, exerciseRepo)
	statsHandler := handlers.NewStatsHandler(statsRepo)

	// Настраиваем роутер
	router := setupRouter(cfg, exerciseHandler, userHandler, workoutHandler, statsHandler)

	// Запускаем HTTP сервер
	srv := &http.Server{
		Addr:         fmt.Sprintf(":%d", cfg.Server.HTTPPort),
		Handler:      router,
		ReadTimeout:  15 * time.Second,
		WriteTimeout: 15 * time.Second,
		IdleTimeout:  60 * time.Second,
	}

	// Graceful shutdown
	go func() {
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("Failed to start server: %v", err)
		}
	}()

	log.Printf("Server started on port %d", cfg.Server.HTTPPort)

	// Ждем сигналов завершения
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	log.Println("Shutting down server...")

	ctx, cancel := context.WithTimeout(context.Background(), cfg.Server.ShutdownTimeout)
	defer cancel()

	if err := srv.Shutdown(ctx); err != nil {
		log.Fatalf("Server forced to shutdown: %v", err)
	}

	log.Println("Server exited")
}

func initDatabase(cfg *config.Config) *sqlx.DB {
	connStr := fmt.Sprintf(
		"host=%s port=%d user=%s password=%s dbname=%s sslmode=%s",
		cfg.Database.Postgres.Master.Host,
		cfg.Database.Postgres.Master.Port,
		cfg.Database.Postgres.Master.User,
		cfg.Database.Postgres.Master.Password,
		cfg.Database.Postgres.Master.Database,
		cfg.Database.Postgres.Master.SSLMode,
	)

	db, err := sqlx.Connect("postgres", connStr)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	// Настраиваем пул соединений
	db.SetMaxOpenConns(cfg.Database.Postgres.Master.MaxConnections)
	db.SetMaxIdleConns(cfg.Database.Postgres.Master.MaxIdleConnections)
	db.SetConnMaxLifetime(cfg.Database.Postgres.Master.ConnMaxLifetime)

	if err = db.Ping(); err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}

	log.Println("✅ Database connected successfully")
	return db
}

func setupRouter(
	cfg *config.Config,
	exerciseHandler *handlers.ExerciseHandler,
	userHandler *handlers.UserHandler,
	workoutHandler *handlers.WorkoutHandler,
	statsHandler *handlers.StatsHandler,
) *gin.Engine {
	if cfg.Server.Environment == "production" {
		gin.SetMode(gin.ReleaseMode)
	}

	router := gin.New()
	router.Use(gin.Recovery())
	router.Use(gin.LoggerWithConfig(gin.LoggerConfig{
		SkipPaths: []string{"/health", "/metrics"},
	}))

	// ============ МЕТРИКИ (ДО СТАТИКИ!) ============
	router.GET("/metrics", gin.WrapH(promhttp.Handler()))

	// ============ ПУБЛИЧНЫЕ API МАРШРУТЫ (ДО СТАТИКИ!) ============
	public := router.Group("/api")
	{
		public.POST("/register", userHandler.Register)
		public.POST("/login", userHandler.Login)
		public.GET("/health", healthHandler(cfg))
		public.GET("/health/detailed", detailedHealthHandler(cfg))
		public.GET("/user/check", userHandler.CheckUser)
		public.GET("/user/check/email", userHandler.CheckEmail)
		public.GET("/user/check/username", userHandler.CheckUsername)
	}

	// ============ ЗАЩИЩЕННЫЕ API МАРШРУТЫ (ДО СТАТИКИ!) ============
	protected := router.Group("/api")
	protected.Use(middleware.AuthMiddleware(cfg.Auth.JWTSecret))
	{
		protected.GET("/profile", userHandler.GetProfile)
		protected.PUT("/profile", userHandler.UpdateProfile)
		protected.POST("/change-password", userHandler.ChangePassword)

		protected.GET("/exercises", workoutHandler.GetExercises)
		protected.GET("/exercises/:id", workoutHandler.GetExercise)
		protected.GET("/get_exercise_list", exerciseHandler.GetExerciseListFromDB)

		protected.GET("/exercise_state", exerciseHandler.GetExerciseState)
		protected.POST("/exercise/reset", exerciseHandler.ResetExercise)

		protected.POST("/workout/start", workoutHandler.StartWorkout)
		protected.POST("/workout/end", workoutHandler.EndWorkout)
		protected.POST("/workout/exercise", workoutHandler.AddExerciseSet)
		protected.GET("/workout/history", workoutHandler.GetWorkoutHistory)
		protected.GET("/workout/current", workoutHandler.GetCurrentWorkout)
		protected.GET("/workout/session/:id", workoutHandler.GetSessionDetails)

		protected.GET("/stats/overall", statsHandler.GetOverallStats)
		protected.GET("/stats/daily", statsHandler.GetDailyStats)
		protected.GET("/stats/weekly", statsHandler.GetWeeklyStats)
		protected.GET("/stats/monthly", statsHandler.GetMonthlyStats)
		protected.GET("/stats/exercises", statsHandler.GetExerciseStats)
		protected.GET("/stats/exercises/:id", statsHandler.GetExerciseStatsByID)
		protected.GET("/stats/period", statsHandler.GetStatsForPeriod)
		protected.GET("/stats/history", statsHandler.GetWorkoutHistory)

		protected.GET("/dashboard", statsHandler.GetDashboard)
	}

	// ============ WEBSOCKET МАРШРУТЫ ============
	ws := router.Group("/ws")
	ws.Use(middleware.AuthMiddleware(cfg.Auth.JWTSecret))
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

	// ============ СТАТИЧЕСКИЕ ФАЙЛЫ (для всех не найденных маршрутов) ============
	wd, err := os.Getwd()
	if err != nil {
		log.Printf("Error getting working directory: %v", err)
	} else {
		webPath := filepath.Join(wd, "web")

		// Проверяем существование папки web
		if _, err := os.Stat(webPath); os.IsNotExist(err) {
			webPath = wd
			log.Printf("Web folder not found, using current directory: %s", wd)
		}

		// Используем NoRoute для обработки всех не найденных маршрутов
		router.NoRoute(func(c *gin.Context) {
			// Проверяем, не API ли это (на всякий случай)
			path := c.Request.URL.Path
			if strings.HasPrefix(path, "/api") ||
				strings.HasPrefix(path, "/ws") ||
				strings.HasPrefix(path, "/metrics") {
				c.JSON(404, gin.H{"error": "Not found"})
				return
			}

			// Пытаемся найти файл в webPath
			filePath := filepath.Join(webPath, path)
			if _, err := os.Stat(filePath); err == nil {
				c.File(filePath)
				return
			}

			// Если файл не найден, отдаем index.html (для SPA)
			indexPath := filepath.Join(webPath, "index.html")
			if _, err := os.Stat(indexPath); err == nil {
				c.File(indexPath)
				return
			}

			// Если ничего не нашли - 404
			c.String(404, "Not found")
		})

		log.Printf("✅ Serving static files from: %s", webPath)
	}

	return router
}

func healthHandler(cfg *config.Config) gin.HandlerFunc {
	return func(ctx *gin.Context) {
		ctx.JSON(http.StatusOK, gin.H{
			"status":    "ok",
			"server_id": cfg.Server.Instances.ID,
			"time":      time.Now().Unix(),
		})
	}
}

func detailedHealthHandler(cfg *config.Config) gin.HandlerFunc {
	return func(ctx *gin.Context) {
		ctx.JSON(http.StatusOK, gin.H{
			"status":      "ok",
			"server_id":   cfg.Server.Instances.ID,
			"environment": cfg.Server.Environment,
			"version":     "1.0.0",
			"time":        time.Now().Unix(),
			"checks": gin.H{
				"database": "ok",
				"redis":    "ok",
				"python":   "ok",
			},
		})
	}
}
