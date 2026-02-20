package repository

import (
	"database/sql"
	"lfk-backend/internal/models"
	"log"
	"time"

	"github.com/google/uuid"
	"github.com/jmoiron/sqlx"
)

type StatsRepository struct {
	db *sqlx.DB
}

func NewStatsRepository(db *sqlx.DB) *StatsRepository {
	return &StatsRepository{db: db}
}

// UpdateExerciseStats обновляет статистику после выполнения упражнения
func (r *StatsRepository) UpdateExerciseStats(userID, exerciseID string, repetitions, duration int, accuracy float64) error {
	tx, err := r.db.Beginx()
	if err != nil {
		return err
	}
	defer tx.Rollback()

	now := time.Now()
	today := now.Format("2006-01-02")

	// 1. Обновляем или создаем запись в exercise_stats
	var exists bool
	query := `SELECT EXISTS(SELECT 1 FROM exercise_stats WHERE user_id = $1 AND exercise_id = $2)`
	err = tx.Get(&exists, query, userID, exerciseID)
	if err != nil {
		return err
	}

	if exists {
		// Обновляем существующую запись
		query = `
			UPDATE exercise_stats 
			SET total_sessions = total_sessions + 1,
				total_repetitions = total_repetitions + $3,
				total_duration = total_duration + $4,
				best_accuracy = GREATEST(best_accuracy, $5),
				avg_accuracy = (avg_accuracy * total_sessions + $5) / (total_sessions + 1),
				last_performed_at = $6,
				updated_at = NOW()
			WHERE user_id = $1 AND exercise_id = $2
		`
		_, err = tx.Exec(query, userID, exerciseID, repetitions, duration, accuracy, now)
	} else {
		// Создаем новую запись
		query = `
			INSERT INTO exercise_stats (
				id, user_id, exercise_id, total_sessions, total_repetitions,
				total_duration, best_accuracy, avg_accuracy, last_performed_at,
				created_at, updated_at
			) VALUES ($1, $2, $3, 1, $4, $5, $6, $6, $7, NOW(), NOW())
		`
		_, err = tx.Exec(query, uuid.New().String(), userID, exerciseID, repetitions, duration, accuracy, now)
	}
	if err != nil {
		return err
	}

	// 2. Обновляем ежедневную статистику
	query = `
		INSERT INTO daily_stats (
			id, user_id, stat_date, total_sessions, total_exercises,
			total_duration_seconds, calories_burned, created_at, updated_at
		) VALUES ($1, $2, $3, 1, 1, $4, $5, NOW(), NOW())
		ON CONFLICT (user_id, stat_date) DO UPDATE SET
			total_sessions = daily_stats.total_sessions + 1,
			total_exercises = daily_stats.total_exercises + 1,
			total_duration_seconds = daily_stats.total_duration_seconds + $4,
			calories_burned = daily_stats.calories_burned + $5,
			updated_at = NOW()
	`

	// Примерный расчет калорий (0.05 калорий в секунду)
	calories := float64(duration) * 0.05

	_, err = tx.Exec(query, uuid.New().String(), userID, today, duration, calories)
	if err != nil {
		return err
	}

	// 3. Обновляем общую статистику
	query = `
		INSERT INTO overall_stats (
			user_id, total_sessions, total_exercises, total_repetitions,
			total_duration, last_workout_at, created_at, updated_at
		) VALUES ($1, 1, 1, $2, $3, $4, NOW(), NOW())
		ON CONFLICT (user_id) DO UPDATE SET
			total_sessions = overall_stats.total_sessions + 1,
			total_exercises = overall_stats.total_exercises + 1,
			total_repetitions = overall_stats.total_repetitions + $2,
			total_duration = overall_stats.total_duration + $3,
			last_workout_at = $4,
			updated_at = NOW()
	`

	_, err = tx.Exec(query, userID, repetitions, duration, now)
	if err != nil {
		return err
	}

	// 4. Обновляем unique_exercises
	query = `
		UPDATE overall_stats 
		SET unique_exercises = (
			SELECT COUNT(DISTINCT exercise_id) 
			FROM exercise_stats 
			WHERE user_id = $1
		)
		WHERE user_id = $1
	`
	_, err = tx.Exec(query, userID)
	if err != nil {
		return err
	}

	// 5. Обновляем streak
	if err := r.updateStreak(tx, userID, today); err != nil {
		return err
	}

	return tx.Commit()
}

// updateStreak обновляет серию тренировок
func (r *StatsRepository) updateStreak(tx *sqlx.Tx, userID string, today string) error {
	// Проверяем, была ли тренировка вчера
	yesterday := time.Now().AddDate(0, 0, -1).Format("2006-01-02")

	var hasYesterday bool
	query := `SELECT EXISTS(SELECT 1 FROM daily_stats WHERE user_id = $1 AND stat_date = $2)`
	err := tx.Get(&hasYesterday, query, userID, yesterday)
	if err != nil {
		return err
	}

	// Получаем текущий streak
	var currentStreak int
	query = `SELECT COALESCE(current_streak, 0) FROM overall_stats WHERE user_id = $1`
	err = tx.Get(&currentStreak, query, userID)
	if err != nil && err != sql.ErrNoRows {
		return err
	}

	var newStreak int
	if hasYesterday {
		newStreak = currentStreak + 1
	} else {
		newStreak = 1
	}

	// Обновляем streak
	query = `
		UPDATE overall_stats SET 
			current_streak = $2,
			longest_streak = GREATEST(longest_streak, $2),
			updated_at = NOW()
		WHERE user_id = $1
	`
	_, err = tx.Exec(query, userID, newStreak)
	return err
}

// GetOverallStats получает общую статистику пользователя
func (r *StatsRepository) GetOverallStats(userID string) (*models.OverallStats, error) {
	log.Printf("🔍 Querying overall_stats for user: %s", userID)

	var stats models.OverallStats
	query := `SELECT * FROM overall_stats WHERE user_id = $1`
	err := r.db.Get(&stats, query, userID)
	if err != nil {
		if err == sql.ErrNoRows {
			log.Printf("ℹ️ No overall_stats found for user %s", userID)
			return nil, nil
		}
		log.Printf("❌ Database error in GetOverallStats: %v", err)
		return nil, err
	}

	log.Printf("✅ Found overall_stats: %+v", stats)
	return &stats, nil
}

// GetDailyStats получает статистику за конкретный день
func (r *StatsRepository) GetDailyStats(userID string, date time.Time) (*models.DailyStats, error) {
	var stats models.DailyStats
	query := `SELECT * FROM daily_stats WHERE user_id = $1 AND stat_date = $2`
	err := r.db.Get(&stats, query, userID, date.Format("2006-01-02"))
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &stats, nil
}

// GetWeeklyStats получает статистику за последние 7 дней
func (r *StatsRepository) GetWeeklyStats(userID string) ([]models.DailyStats, error) {
	var stats []models.DailyStats
	query := `
		SELECT * FROM daily_stats 
		WHERE user_id = $1 
		AND stat_date >= CURRENT_DATE - INTERVAL '7 days'
		ORDER BY stat_date DESC
	`
	err := r.db.Select(&stats, query, userID)
	return stats, err
}

// GetMonthlyStats получает статистику за последние 30 дней
func (r *StatsRepository) GetMonthlyStats(userID string) ([]models.DailyStats, error) {
	var stats []models.DailyStats
	query := `
		SELECT * FROM daily_stats 
		WHERE user_id = $1 
		AND stat_date >= CURRENT_DATE - INTERVAL '30 days'
		ORDER BY stat_date DESC
	`
	err := r.db.Select(&stats, query, userID)
	return stats, err
}

// GetExerciseStats получает статистику по всем упражнениям
func (r *StatsRepository) GetExerciseStats(userID string) ([]models.ExerciseStats, error) {
	var stats []models.ExerciseStats
	query := `
		SELECT es.*, e.name as exercise_name 
		FROM exercise_stats es
		JOIN exercises e ON e.id = es.exercise_id
		WHERE es.user_id = $1
		ORDER BY es.total_sessions DESC
	`
	err := r.db.Select(&stats, query, userID)
	return stats, err
}

// GetExerciseStatsByID получает статистику по конкретному упражнению
func (r *StatsRepository) GetExerciseStatsByID(userID, exerciseID string) (*models.ExerciseStats, error) {
	var stats models.ExerciseStats
	query := `
		SELECT es.*, e.name as exercise_name 
		FROM exercise_stats es
		JOIN exercises e ON e.id = es.exercise_id
		WHERE es.user_id = $1 AND es.exercise_id = $2
	`
	err := r.db.Get(&stats, query, userID, exerciseID)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &stats, nil
}

// GetWorkoutHistory получает историю тренировок
func (r *StatsRepository) GetWorkoutHistory(userID string, limit int) ([]models.WorkoutSummary, error) {
	var history []models.WorkoutSummary
	query := `
		SELECT 
			ws.id,
			ws.started_at as date,
			COUNT(DISTINCT es.exercise_id) as exercises,
			COALESCE(SUM(es.actual_repetitions), 0) as repetitions,
			COALESCE(SUM(es.actual_duration_seconds), 0) as duration,
			COALESCE(AVG(es.accuracy_score), 0) as accuracy
		FROM workout_sessions ws
		LEFT JOIN exercise_sets es ON es.session_id = ws.id
		WHERE ws.user_id = $1 AND ws.status = 'completed'
		GROUP BY ws.id, ws.started_at
		ORDER BY ws.started_at DESC
		LIMIT $2
	`
	err := r.db.Select(&history, query, userID, limit)
	return history, err
}

// GetStatsForPeriod получает статистику за период
func (r *StatsRepository) GetStatsForPeriod(userID string, startDate, endDate time.Time) (*models.StatsPeriod, error) {
	var period models.StatsPeriod
	period.StartDate = startDate
	period.EndDate = endDate

	query := `
		SELECT 
			COALESCE(SUM(total_sessions), 0) as total_sessions,
			COALESCE(SUM(total_exercises), 0) as total_exercises,
			COALESCE(SUM(total_duration_seconds), 0) as total_duration
		FROM daily_stats
		WHERE user_id = $1 AND stat_date BETWEEN $2 AND $3
	`
	err := r.db.Get(&period, query, userID, startDate.Format("2006-01-02"), endDate.Format("2006-01-02"))
	if err != nil && err != sql.ErrNoRows {
		return nil, err
	}

	days := int(endDate.Sub(startDate).Hours()/24) + 1
	if days > 0 {
		period.AveragePerDay = float64(period.TotalDuration) / float64(days)
	}

	return &period, nil
}
