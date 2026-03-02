package models

import (
	"database/sql"
	"time"

	"github.com/lib/pq"
)

// Типы упражнений
type ExerciseType string

// Структурированные данные для упражнения
type StructuredData struct {
	State       interface{} `json:"state"` // может быть строкой или числом
	StateName   string      `json:"state_name"`
	Countdown   *int        `json:"countdown,omitempty"`
	Progress    float64     `json:"progress_percent"`
	Cycle       int         `json:"current_cycle"`
	TotalCycles int         `json:"total_cycles"`
	Status      string      `json:"status,omitempty"`
	Completed   bool        `json:"completed"`
	Message     string      `json:"message"`
	Step        int         `json:"step,omitempty"`
	StepName    string      `json:"step_name,omitempty"`
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

// ExerciseInfo - информация об упражнении для списка
type ExerciseInfo struct {
	ID              string         `db:"id" json:"exercise_id"`
	Name            string         `db:"name" json:"name"`
	Description     string         `db:"description" json:"description"`
	Category        string         `db:"category" json:"category"`
	CategoryID      int            `db:"category_id" json:"category_id"`
	CategoryIcon    string         `db:"category_icon" json:"category_icon"`
	CategoryColor   string         `db:"category_color" json:"category_color"`
	DifficultyLevel int            `db:"difficulty_level" json:"difficulty_level"`
	TargetMuscles   pq.StringArray `db:"target_muscles" json:"target_muscles"` // Используем pq.StringArray
	Instructions    pq.StringArray `db:"instructions" json:"instructions"`     // Используем pq.StringArray
	DurationSeconds int            `db:"duration_seconds" json:"duration_seconds"`
	ImageURL        *string        `db:"image_url" json:"image_url"`
	VideoURL        *string        `db:"video_url" json:"video_url"`
}

// ExerciseListResponse - ответ со списком упражнений
type ExerciseListResponse struct {
	Items []ExerciseInfo `json:"items"`
}

// Exercise модель упражнения (полная)
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

// Category модель категории упражнений
type Category struct {
	ID          int       `db:"id" json:"id"`
	Name        string    `db:"name" json:"name"`
	Description string    `db:"description" json:"description"`
	Icon        string    `db:"icon" json:"icon"`
	Color       string    `db:"color" json:"color"`
	CreatedAt   time.Time `db:"created_at" json:"created_at"`
}
