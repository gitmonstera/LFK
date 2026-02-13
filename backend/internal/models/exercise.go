package models

import "time"

type ExerciseType string

const (
	FistExercise ExerciseType = "fist"
)

type ExerciseSession struct {
	ID        string       `json:"id"`
	UserID    string       `json:"userId"`
	Type      ExerciseType `json:"type"`
	StartTime time.Time    `json:"startTime"`
	EndTime   *time.Time   `json:"endTime,omitempty"`
	IsActive  bool         `json:"isActive"`
}

type FrameFeedback struct {
	FistDetected   bool   `json:"fist_detected"`
	HandDetected   bool   `json:"hand_detected"`
	RaisedFingers  int    `json:"raised_fingers"`
	Message        string `json:"message"`
	ProcessedFrame string `json:"processed_frame"`
	Timestamp      int64  `json:"timestamp"`
}

type ExerciseRequest struct {
	ExerciseType ExerciseType `json:"exerciseType" binding:"required"`
}
