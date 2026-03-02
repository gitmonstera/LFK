package models

import (
	"database/sql"
	"time"
)

// ExerciseSet - модель выполнения упражнения
type ExerciseSet struct {
	ID                    string          `db:"id" json:"id"`
	SessionID             string          `db:"session_id" json:"session_id"`
	ExerciseID            string          `db:"exercise_id" json:"exercise_id"`
	StartedAt             time.Time       `db:"started_at" json:"started_at"`
	CompletedAt           sql.NullTime    `db:"completed_at" json:"completed_at,omitempty"`
	TargetRepetitions     sql.NullInt64   `db:"target_repetitions" json:"target_repetitions,omitempty"`
	ActualRepetitions     sql.NullInt64   `db:"actual_repetitions" json:"actual_repetitions"`
	TargetDurationSeconds sql.NullInt64   `db:"target_duration_seconds" json:"target_duration_seconds,omitempty"`
	ActualDurationSeconds sql.NullInt64   `db:"actual_duration_seconds" json:"actual_duration_seconds"`
	AccuracyScore         sql.NullFloat64 `db:"accuracy_score" json:"accuracy_score,omitempty"`
	CompletionStatus      string          `db:"completion_status" json:"completion_status"`
	PerformanceData       []byte          `db:"performance_data" json:"performance_data,omitempty"`
	Notes                 sql.NullString  `db:"notes" json:"notes,omitempty"`
	CreatedAt             time.Time       `db:"created_at" json:"created_at"`
}
