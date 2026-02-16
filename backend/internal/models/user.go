package models

import (
	"database/sql"
	"time"
)

type User struct {
	ID           string          `db:"id" json:"id"`
	Username     string          `db:"username" json:"username"`
	Email        string          `db:"email" json:"email"`
	PasswordHash string          `db:"password_hash" json:"-"`
	FirstName    sql.NullString  `db:"first_name" json:"first_name,omitempty"`
	LastName     sql.NullString  `db:"last_name" json:"last_name,omitempty"`
	BirthDate    sql.NullTime    `db:"birth_date" json:"birth_date,omitempty"`
	Gender       sql.NullString  `db:"gender" json:"gender,omitempty"`
	HeightCm     sql.NullInt64   `db:"height_cm" json:"height_cm,omitempty"`
	WeightKg     sql.NullFloat64 `db:"weight_kg" json:"weight_kg,omitempty"`
	CreatedAt    time.Time       `db:"created_at" json:"created_at"`
	UpdatedAt    time.Time       `db:"updated_at" json:"updated_at"`
	LastLogin    sql.NullTime    `db:"last_login" json:"last_login,omitempty"`
	IsActive     bool            `db:"is_active" json:"is_active"`
	Role         string          `db:"role" json:"role"`
}
