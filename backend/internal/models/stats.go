package models

import (
	"database/sql"
	"time"
)

// ExerciseStats - статистика по конкретному упражнению
type ExerciseStats struct {
	ID               string          `db:"id" json:"id"`
	UserID           string          `db:"user_id" json:"user_id"`
	ExerciseID       string          `db:"exercise_id" json:"exercise_id"`
	ExerciseName     string          `db:"exercise_name" json:"exercise_name,omitempty"`
	TotalSessions    int             `db:"total_sessions" json:"total_sessions"`
	TotalRepetitions int             `db:"total_repetitions" json:"total_repetitions"`
	TotalDuration    int             `db:"total_duration" json:"total_duration"`
	BestAccuracy     sql.NullFloat64 `db:"best_accuracy" json:"best_accuracy,omitempty"`
	AvgAccuracy      sql.NullFloat64 `db:"avg_accuracy" json:"avg_accuracy,omitempty"`
	LastPerformedAt  sql.NullTime    `db:"last_performed_at" json:"last_performed_at,omitempty"`
	CreatedAt        time.Time       `db:"created_at" json:"created_at"`
	UpdatedAt        time.Time       `db:"updated_at" json:"updated_at"`
}

// DailyStats - ежедневная статистика
type DailyStats struct {
	ID                   string    `db:"id" json:"id"`
	UserID               string    `db:"user_id" json:"user_id"`
	StatDate             time.Time `db:"stat_date" json:"stat_date"`
	TotalSessions        int       `db:"total_sessions" json:"total_sessions"`
	TotalDurationSeconds int       `db:"total_duration_seconds" json:"total_duration_seconds"`
	TotalExercises       int       `db:"total_exercises" json:"total_exercises"`
	CaloriesBurned       float64   `db:"calories_burned" json:"calories_burned"`
	StreakDay            int       `db:"streak_day" json:"streak_day"`
	Completed            bool      `db:"completed" json:"completed"`
	CreatedAt            time.Time `db:"created_at" json:"created_at"`
	UpdatedAt            time.Time `db:"updated_at" json:"updated_at"`
}

// OverallStats - общая статистика пользователя
type OverallStats struct {
	UserID           string       `db:"user_id" json:"user_id"`
	TotalSessions    int          `db:"total_sessions" json:"total_sessions"`
	TotalExercises   int          `db:"total_exercises" json:"total_exercises"`
	TotalRepetitions int          `db:"total_repetitions" json:"total_repetitions"`
	TotalDuration    int          `db:"total_duration" json:"total_duration"`
	UniqueExercises  int          `db:"unique_exercises" json:"unique_exercises"`
	CurrentStreak    int          `db:"current_streak" json:"current_streak"`
	LongestStreak    int          `db:"longest_streak" json:"longest_streak"`
	JoinedAt         time.Time    `db:"joined_at" json:"joined_at"`
	LastWorkoutAt    sql.NullTime `db:"last_workout_at" json:"last_workout_at,omitempty"`
	CreatedAt        time.Time    `db:"created_at" json:"created_at"`
	UpdatedAt        time.Time    `db:"updated_at" json:"updated_at"`
}

// Goal - цель пользователя
type Goal struct {
	ID             string    `db:"id" json:"id"`
	UserID         string    `db:"user_id" json:"user_id"`
	DailyMinutes   int       `db:"daily_minutes" json:"daily_minutes"`
	DailyExercises int       `db:"daily_exercises" json:"daily_exercises"`
	WeeklySessions int       `db:"weekly_sessions" json:"weekly_sessions"`
	CreatedAt      time.Time `db:"created_at" json:"created_at"`
	UpdatedAt      time.Time `db:"updated_at" json:"updated_at"`
}

// WorkoutSummary - краткая информация о тренировке
type WorkoutSummary struct {
	ID          string    `json:"id"`
	Date        time.Time `json:"date"`
	Exercises   int       `json:"exercises"`
	Repetitions int       `json:"repetitions"`
	Duration    int       `json:"duration"`
	Accuracy    float64   `json:"accuracy"`
}

// StatsPeriod - статистика за период
type StatsPeriod struct {
	StartDate      time.Time `json:"start_date"`
	EndDate        time.Time `json:"end_date"`
	TotalSessions  int       `json:"total_sessions"`
	TotalExercises int       `json:"total_exercises"`
	TotalDuration  int       `json:"total_duration"`
	AveragePerDay  float64   `json:"average_per_day"`
}
