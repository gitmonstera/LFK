package handlers

import (
	"lfk-backend/internal/models"
	"lfk-backend/internal/repository"
	"log"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
)

type StatsHandler struct {
	statsRepo *repository.StatsRepository
}

func NewStatsHandler(statsRepo *repository.StatsRepository) *StatsHandler {
	return &StatsHandler{
		statsRepo: statsRepo,
	}
}

// GetOverallStats получение общей статистики
func (h *StatsHandler) GetOverallStats(c *gin.Context) {
	userID := c.GetString("user_id")

	// Добавляем подробное логирование
	log.Printf("📊 GetOverallStats called for user: %s", userID)

	stats, err := h.statsRepo.GetOverallStats(userID)
	if err != nil {
		log.Printf("❌ Error getting overall stats: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get stats",
			"detail": err.Error(),
		})
		return
	}

	log.Printf("✅ Overall stats retrieved: %+v", stats)

	// Если статистики нет, возвращаем пустую статистику с нулевыми значениями
	if stats == nil {
		log.Printf("ℹ️ No stats found for user %s, returning empty stats", userID)
		c.JSON(http.StatusOK, models.OverallStats{
			UserID:           userID,
			TotalSessions:    0,
			TotalExercises:   0,
			TotalRepetitions: 0,
			TotalDuration:    0,
			UniqueExercises:  0,
			CurrentStreak:    0,
			LongestStreak:    0,
			JoinedAt:         time.Now(),
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetExerciseStats получение статистики по упражнениям
func (h *StatsHandler) GetExerciseStats(c *gin.Context) {
	userID := c.GetString("user_id")

	stats, err := h.statsRepo.GetExerciseStats(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get exercise stats",
			"detail": err.Error(),
		})
		return
	}

	// Если статистики нет, возвращаем пустой массив
	if stats == nil {
		c.JSON(http.StatusOK, []models.ExerciseStats{})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetDailyStats получение статистики за день
func (h *StatsHandler) GetDailyStats(c *gin.Context) {
	userID := c.GetString("user_id")

	dateStr := c.Query("date")
	var date time.Time
	var err error

	if dateStr == "" {
		date = time.Now()
	} else {
		date, err = time.Parse("2006-01-02", dateStr)
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid date format. Use YYYY-MM-DD"})
			return
		}
	}

	stats, err := h.statsRepo.GetDailyStats(userID, date)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get daily stats",
			"detail": err.Error(),
		})
		return
	}

	// Если статистики нет, возвращаем пустую статистику
	if stats == nil {
		c.JSON(http.StatusOK, models.DailyStats{
			UserID:               userID,
			StatDate:             date,
			TotalSessions:        0,
			TotalExercises:       0,
			TotalDurationSeconds: 0,
			CaloriesBurned:       0,
			StreakDay:            0,
			Completed:            false,
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetWeeklyStats получение статистики за неделю
func (h *StatsHandler) GetWeeklyStats(c *gin.Context) {
	userID := c.GetString("user_id")

	stats, err := h.statsRepo.GetWeeklyStats(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get weekly stats",
			"detail": err.Error(),
		})
		return
	}

	if stats == nil {
		c.JSON(http.StatusOK, []models.DailyStats{})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetMonthlyStats получение статистики за месяц
func (h *StatsHandler) GetMonthlyStats(c *gin.Context) {
	userID := c.GetString("user_id")

	stats, err := h.statsRepo.GetMonthlyStats(userID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get monthly stats",
			"detail": err.Error(),
		})
		return
	}

	if stats == nil {
		c.JSON(http.StatusOK, []models.DailyStats{})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetExerciseStatsByID получение статистики по конкретному упражнению
func (h *StatsHandler) GetExerciseStatsByID(c *gin.Context) {
	userID := c.GetString("user_id")
	exerciseID := c.Param("id")

	stats, err := h.statsRepo.GetExerciseStatsByID(userID, exerciseID)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get exercise stats",
			"detail": err.Error(),
		})
		return
	}

	if stats == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "No stats for this exercise"})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetWorkoutHistory получение истории тренировок
func (h *StatsHandler) GetWorkoutHistory(c *gin.Context) {
	userID := c.GetString("user_id")

	limitStr := c.DefaultQuery("limit", "10")
	limit, err := strconv.Atoi(limitStr)
	if err != nil || limit <= 0 {
		limit = 10
	}
	if limit > 100 {
		limit = 100
	}

	history, err := h.statsRepo.GetWorkoutHistory(userID, limit)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get workout history",
			"detail": err.Error(),
		})
		return
	}

	if history == nil {
		c.JSON(http.StatusOK, []models.WorkoutSummary{})
		return
	}

	c.JSON(http.StatusOK, history)
}

// GetStatsForPeriod получение статистики за период
func (h *StatsHandler) GetStatsForPeriod(c *gin.Context) {
	userID := c.GetString("user_id")

	startStr := c.Query("start")
	endStr := c.Query("end")

	if startStr == "" || endStr == "" {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Start and end dates are required"})
		return
	}

	start, err := time.Parse("2006-01-02", startStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid start date format. Use YYYY-MM-DD"})
		return
	}

	end, err := time.Parse("2006-01-02", endStr)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid end date format. Use YYYY-MM-DD"})
		return
	}

	if end.Before(start) {
		c.JSON(http.StatusBadRequest, gin.H{"error": "End date must be after start date"})
		return
	}

	stats, err := h.statsRepo.GetStatsForPeriod(userID, start, end)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error":  "Failed to get stats",
			"detail": err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, stats)
}

// GetDashboard получение данных для дашборда
func (h *StatsHandler) GetDashboard(c *gin.Context) {
	userID := c.GetString("user_id")

	// Получаем все данные параллельно
	type result struct {
		overall   *models.OverallStats
		weekly    []models.DailyStats
		monthly   []models.DailyStats
		exercises []models.ExerciseStats
		err       error
	}

	ch := make(chan result, 4)

	go func() {
		stats, err := h.statsRepo.GetOverallStats(userID)
		ch <- result{overall: stats, err: err}
	}()

	go func() {
		stats, err := h.statsRepo.GetWeeklyStats(userID)
		ch <- result{weekly: stats, err: err}
	}()

	go func() {
		stats, err := h.statsRepo.GetMonthlyStats(userID)
		ch <- result{monthly: stats, err: err}
	}()

	go func() {
		stats, err := h.statsRepo.GetExerciseStats(userID)
		ch <- result{exercises: stats, err: err}
	}()

	dashboard := gin.H{}

	for i := 0; i < 4; i++ {
		res := <-ch
		if res.err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"error":  "Failed to load dashboard data",
				"detail": res.err.Error(),
			})
			return
		}
		if res.overall != nil {
			dashboard["overall"] = res.overall
		}
		if res.weekly != nil {
			dashboard["weekly"] = res.weekly
		}
		if res.monthly != nil {
			dashboard["monthly"] = res.monthly
		}
		if res.exercises != nil {
			dashboard["exercises"] = res.exercises
		}
	}

	// Добавляем пустые значения, если каких-то данных нет
	if dashboard["overall"] == nil {
		dashboard["overall"] = models.OverallStats{
			UserID:   userID,
			JoinedAt: time.Now(),
		}
	}
	if dashboard["weekly"] == nil {
		dashboard["weekly"] = []models.DailyStats{}
	}
	if dashboard["monthly"] == nil {
		dashboard["monthly"] = []models.DailyStats{}
	}
	if dashboard["exercises"] == nil {
		dashboard["exercises"] = []models.ExerciseStats{}
	}

	c.JSON(http.StatusOK, dashboard)
}
