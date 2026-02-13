package models

import "time"

// Типы упражнений
type ExerciseType string

const (
	FistExercise ExerciseType = "fist"
)

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
	FistDetected   bool   `json:"fist_detected"`
	HandDetected   bool   `json:"hand_detected"`
	RaisedFingers  int    `json:"raised_fingers"`
	FingerStates   []bool `json:"finger_states"`
	Message        string `json:"message"`
	ProcessedFrame string `json:"processed_frame"`
	Timestamp      int64  `json:"timestamp"`
}

// Запрос на начало упражнения
type ExerciseRequest struct {
	ExerciseType ExerciseType `json:"exerciseType" binding:"required"`
}
