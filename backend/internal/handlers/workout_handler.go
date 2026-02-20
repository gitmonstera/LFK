package handlers

import (
	"database/sql"
	"lfk-backend/internal/models"
	"lfk-backend/internal/repository"
	"log"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
)

type WorkoutHandler struct {
	workoutRepo  *repository.WorkoutRepository
	statsRepo    *repository.StatsRepository
	exerciseRepo *repository.ExerciseRepository
}

func NewWorkoutHandler(
	workoutRepo *repository.WorkoutRepository,
	statsRepo *repository.StatsRepository,
	exerciseRepo *repository.ExerciseRepository,
) *WorkoutHandler {
	return &WorkoutHandler{
		workoutRepo:  workoutRepo,
		statsRepo:    statsRepo,
		exerciseRepo: exerciseRepo,
	}
}

// GetExercises возвращает список всех упражнений
func (h *WorkoutHandler) GetExercises(c *gin.Context) {
	exercises, err := h.exerciseRepo.GetAll()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get exercises"})
		return
	}
	c.JSON(http.StatusOK, exercises)
}

// GetExercise возвращает упражнение по ID
func (h *WorkoutHandler) GetExercise(c *gin.Context) {
	id := c.Param("id")
	exercise, err := h.exerciseRepo.GetByID(id)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Exercise not found"})
		return
	}
	c.JSON(http.StatusOK, exercise)
}

// StartWorkout начинает новую тренировку
func (h *WorkoutHandler) StartWorkout(c *gin.Context) {
	userID := c.GetString("user_id")

	session, err := h.workoutRepo.CreateSession(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to start workout"})
		return
	}

	c.JSON(http.StatusOK, session)
}

// EndWorkout завершает тренировку
type EndWorkoutRequest struct {
	SessionID string `json:"session_id" binding:"required"`
}

func (h *WorkoutHandler) EndWorkout(c *gin.Context) {
	var req EndWorkoutRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	if err := h.workoutRepo.EndSession(req.SessionID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to end workout"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Workout ended successfully"})
}

// AddExerciseSet добавляет выполненное упражнение
type AddExerciseSetRequest struct {
	SessionID         string   `json:"session_id" binding:"required"`
	ExerciseID        string   `json:"exercise_id" binding:"required"`
	ActualRepetitions int      `json:"actual_repetitions" binding:"required,min=1"`
	ActualDuration    int      `json:"actual_duration" binding:"required,min=1"`
	AccuracyScore     *float64 `json:"accuracy_score"`
	TargetRepetitions *int     `json:"target_repetitions"`
	TargetDuration    *int     `json:"target_duration"`
}

func (h *WorkoutHandler) AddExerciseSet(c *gin.Context) {
	var req AddExerciseSetRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		log.Printf("❌ Invalid request in AddExerciseSet: %v", err)
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request", "details": err.Error()})
		return
	}

	userID := c.GetString("user_id")
	log.Printf("📝 AddExerciseSet called for user %s, exercise %s, reps=%d, duration=%d",
		userID, req.ExerciseID, req.ActualRepetitions, req.ActualDuration)

	// Создаем ExerciseSet с правильными sql.Null типами
	set := &models.ExerciseSet{
		SessionID:  req.SessionID,
		ExerciseID: req.ExerciseID,
		StartedAt:  time.Now(),
	}

	// Устанавливаем значения с правильными sql.Null типами
	set.ActualRepetitions = sql.NullInt64{
		Int64: int64(req.ActualRepetitions),
		Valid: true,
	}

	set.ActualDurationSeconds = sql.NullInt64{
		Int64: int64(req.ActualDuration),
		Valid: true,
	}

	if req.TargetRepetitions != nil {
		set.TargetRepetitions = sql.NullInt64{
			Int64: int64(*req.TargetRepetitions),
			Valid: true,
		}
	}

	if req.TargetDuration != nil {
		set.TargetDurationSeconds = sql.NullInt64{
			Int64: int64(*req.TargetDuration),
			Valid: true,
		}
	}

	if req.AccuracyScore != nil {
		set.AccuracyScore = sql.NullFloat64{
			Float64: *req.AccuracyScore,
			Valid:   true,
		}
		log.Printf("📊 Accuracy score provided: %.2f", *req.AccuracyScore)
	} else {
		log.Printf("⚠️ No accuracy score provided")
	}

	// Устанавливаем время завершения
	set.CompletedAt = sql.NullTime{
		Time:  set.StartedAt.Add(time.Duration(req.ActualDuration) * time.Second),
		Valid: true,
	}

	log.Printf("💾 Saving exercise set to database...")
	if err := h.workoutRepo.AddExerciseSet(set); err != nil {
		log.Printf("❌ Failed to save exercise set: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to save exercise set"})
		return
	}
	log.Printf("✅ Exercise set saved successfully with ID: %s", set.ID)

	// Обновляем статистику в фоновом режиме
	if req.AccuracyScore != nil {
		log.Printf("📊 Updating statistics in background...")
		go func() {
			err := h.statsRepo.UpdateExerciseStats(
				userID,
				req.ExerciseID,
				req.ActualRepetitions,
				req.ActualDuration,
				*req.AccuracyScore,
			)
			if err != nil {
				log.Printf("❌ Failed to update stats: %v", err)
			} else {
				log.Printf("✅ Stats updated successfully")
			}
		}()
	} else {
		log.Printf("⚠️ Skipping stats update - no accuracy score")
	}

	c.JSON(http.StatusOK, set)
}

// GetWorkoutHistory получает историю тренировок
func (h *WorkoutHandler) GetWorkoutHistory(c *gin.Context) {
	userID := c.GetString("user_id")
	limit := 20

	sessions, err := h.workoutRepo.GetUserSessions(userID, limit)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get workout history"})
		return
	}

	result := make([]gin.H, 0)
	for _, session := range sessions {
		_, sets, err := h.workoutRepo.GetSessionDetails(session.ID)
		if err != nil {
			continue
		}

		exercises := make([]gin.H, 0)
		totalReps := 0
		totalDuration := 0
		var avgAccuracy float64

		for _, set := range sets {
			exercise, _ := h.exerciseRepo.GetByID(set.ExerciseID)
			exerciseName := ""
			if exercise != nil {
				exerciseName = exercise.Name
			}

			// Безопасно получаем значения из sql.Null типов
			reps := 0
			if set.ActualRepetitions.Valid {
				reps = int(set.ActualRepetitions.Int64)
			}

			dur := 0
			if set.ActualDurationSeconds.Valid {
				dur = int(set.ActualDurationSeconds.Int64)
			}

			acc := 0.0
			if set.AccuracyScore.Valid {
				acc = set.AccuracyScore.Float64
			}

			exercises = append(exercises, gin.H{
				"id":          set.ExerciseID,
				"name":        exerciseName,
				"repetitions": reps,
				"duration":    dur,
				"accuracy":    acc,
			})

			totalReps += reps
			totalDuration += dur
			if set.AccuracyScore.Valid {
				avgAccuracy += set.AccuracyScore.Float64
			}
		}

		if len(sets) > 0 {
			avgAccuracy = avgAccuracy / float64(len(sets))
		}

		result = append(result, gin.H{
			"id":              session.ID,
			"started_at":      session.StartedAt,
			"ended_at":        session.EndedAt,
			"duration":        session.DurationSeconds,
			"exercises":       exercises,
			"total_exercises": len(exercises),
			"total_reps":      totalReps,
			"total_duration":  totalDuration,
			"avg_accuracy":    avgAccuracy,
		})
	}

	c.JSON(http.StatusOK, result)
}

// GetCurrentWorkout получает текущую тренировку
func (h *WorkoutHandler) GetCurrentWorkout(c *gin.Context) {
	userID := c.GetString("user_id")

	session, err := h.workoutRepo.GetActiveSession(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get current workout"})
		return
	}

	if session == nil {
		c.JSON(http.StatusOK, gin.H{"message": "No active workout"})
		return
	}

	_, sets, err := h.workoutRepo.GetSessionDetails(session.ID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to get session details"})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"session":   session,
		"exercises": sets,
	})
}

// GetSessionDetails получает детали сессии
func (h *WorkoutHandler) GetSessionDetails(c *gin.Context) {
	sessionID := c.Param("id")

	session, sets, err := h.workoutRepo.GetSessionDetails(sessionID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Session not found"})
		return
	}

	userID := c.GetString("user_id")
	if session.UserID != userID {
		c.JSON(http.StatusForbidden, gin.H{"error": "Access denied"})
		return
	}

	exercises := make([]gin.H, 0)
	for _, set := range sets {
		exercise, _ := h.exerciseRepo.GetByID(set.ExerciseID)
		exerciseName := ""
		if exercise != nil {
			exerciseName = exercise.Name
		}

		// Безопасно получаем значения из sql.Null типов
		reps := 0
		if set.ActualRepetitions.Valid {
			reps = int(set.ActualRepetitions.Int64)
		}

		dur := 0
		if set.ActualDurationSeconds.Valid {
			dur = int(set.ActualDurationSeconds.Int64)
		}

		acc := 0.0
		if set.AccuracyScore.Valid {
			acc = set.AccuracyScore.Float64
		}

		exercises = append(exercises, gin.H{
			"id":          set.ExerciseID,
			"name":        exerciseName,
			"repetitions": reps,
			"duration":    dur,
			"accuracy":    acc,
			"started_at":  set.StartedAt,
		})
	}

	c.JSON(http.StatusOK, gin.H{
		"session":   session,
		"exercises": exercises,
	})
}
