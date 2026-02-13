package models

import "time"

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
