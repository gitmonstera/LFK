package models

import (
	"database/sql"
	"time"

	"github.com/lib/pq"
)

// Типы упражнений
type ExerciseType string

const (
	FistExercise      ExerciseType = "fist"
	FistIndexExercise ExerciseType = "fist-index"
	FistPalmExercise  ExerciseType = "fist-palm"
)

// Структурированные данные для упражнения Кулак-ладонь
type StructuredData struct {
	Step        int     `json:"step"`
	StepName    string  `json:"step_name"`
	Countdown   *int    `json:"countdown,omitempty"`
	Progress    float64 `json:"progress"`
	Cycle       int     `json:"cycle"`
	TotalCycles int     `json:"total_cycles"`
	Status      string  `json:"status"`
}

// Сессия упражнения
type ExerciseSession struct {
	ID        string       `json:"id"`
	UserID    string       `json:"userId"`
	Type      ExerciseType `json:"type"`
	StartTime time.Time    `json:"startTime"`
	EndTime   *time.Time   `json:"endTime,omitempty"`
	IsActive  bool         `json:"isActive"`
}

// Обратная связь по кадру
type FrameFeedback struct {
	FistDetected    bool            `json:"fist_detected"`
	HandDetected    bool            `json:"hand_detected"`
	RaisedFingers   int             `json:"raised_fingers"`
	FingerStates    []bool          `json:"finger_states"`
	Message         string          `json:"message"`
	ProcessedFrame  string          `json:"processed_frame"`
	CurrentExercise string          `json:"current_exercise"`
	ExerciseName    string          `json:"exercise_name"`
	Structured      *StructuredData `json:"structured,omitempty"`
	Timestamp       int64           `json:"timestamp"`
}

// Запрос на начало упражнения
type ExerciseRequest struct {
	ExerciseType ExerciseType `json:"exerciseType" binding:"required"`
}

// Exercise модель упражнения
type Exercise struct {
	ID              string          `db:"id" json:"id"`
	Name            string          `db:"name" json:"name"`
	Description     string          `db:"description" json:"description"`
	CategoryID      sql.NullInt64   `db:"category_id" json:"category_id"`
	DifficultyLevel sql.NullInt64   `db:"difficulty_level" json:"difficulty_level"`
	TargetMuscles   pq.StringArray  `db:"target_muscles" json:"target_muscles"`
	Instructions    pq.StringArray  `db:"instructions" json:"instructions"`
	DurationSeconds sql.NullInt64   `db:"duration_seconds" json:"duration_seconds"`
	CaloriesBurn    sql.NullFloat64 `db:"calories_burn" json:"calories_burn"`
	VideoURL        sql.NullString  `db:"video_url" json:"video_url"`
	ImageURL        sql.NullString  `db:"image_url" json:"image_url"`
	IsActive        bool            `db:"is_active" json:"is_active"`
	CreatedAt       time.Time       `db:"created_at" json:"created_at"`
	Metadata        []byte          `db:"metadata" json:"metadata,omitempty"`
}
