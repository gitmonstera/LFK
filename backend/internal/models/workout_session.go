package models

import (
	"database/sql"
	"time"
)

// WorkoutSession - модель сессии тренировки
type WorkoutSession struct {
	ID              string         `db:"id" json:"id"`
	UserID          string         `db:"user_id" json:"user_id"`
	StartedAt       time.Time      `db:"started_at" json:"started_at"`
	EndedAt         sql.NullTime   `db:"ended_at" json:"ended_at,omitempty"`
	DurationSeconds sql.NullInt64  `db:"duration_seconds" json:"duration_seconds,omitempty"`
	Status          string         `db:"status" json:"status"`
	Notes           sql.NullString `db:"notes" json:"notes,omitempty"`
	CreatedAt       time.Time      `db:"created_at" json:"created_at"`
}
