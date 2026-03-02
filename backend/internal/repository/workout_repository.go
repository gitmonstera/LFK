package repository

import (
	"database/sql"
	"lfk-backend/internal/models"
	"time"

	"github.com/google/uuid"
	"github.com/jmoiron/sqlx"
)

type WorkoutRepository struct {
	db *sqlx.DB
}

func NewWorkoutRepository(db *sqlx.DB) *WorkoutRepository {
	return &WorkoutRepository{db: db}
}

// CreateSession создает новую сессию тренировки
func (r *WorkoutRepository) CreateSession(userID string) (*models.WorkoutSession, error) {
	session := &models.WorkoutSession{
		ID:        uuid.New().String(),
		UserID:    userID,
		StartedAt: time.Now(),
		Status:    "in_progress",
	}

	query := `
		INSERT INTO workout_sessions (id, user_id, started_at, status)
		VALUES ($1, $2, $3, $4)
		RETURNING created_at
	`
	err := r.db.QueryRow(query, session.ID, session.UserID, session.StartedAt, session.Status).Scan(&session.CreatedAt)
	if err != nil {
		return nil, err
	}
	return session, nil
}

// EndSession завершает сессию тренировки
func (r *WorkoutRepository) EndSession(sessionID string) error {
	endedAt := time.Now()
	query := `
		UPDATE workout_sessions 
		SET ended_at = $1, 
		    duration_seconds = EXTRACT(EPOCH FROM ($1 - started_at))::INTEGER,
		    status = 'completed'
		WHERE id = $2
	`
	_, err := r.db.Exec(query, endedAt, sessionID)
	return err
}

// AddExerciseSet добавляет выполненное упражнение
func (r *WorkoutRepository) AddExerciseSet(set *models.ExerciseSet) error {
	set.ID = uuid.New().String()
	set.CreatedAt = time.Now()

	if set.CompletionStatus == "" {
		set.CompletionStatus = "completed"
	}

	query := `
		INSERT INTO exercise_sets (
			id, session_id, exercise_id, started_at, completed_at,
			target_repetitions, actual_repetitions, 
			target_duration_seconds, actual_duration_seconds,
			accuracy_score, completion_status, notes
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12)
	`
	_, err := r.db.Exec(query,
		set.ID, set.SessionID, set.ExerciseID, set.StartedAt, set.CompletedAt,
		set.TargetRepetitions, set.ActualRepetitions,
		set.TargetDurationSeconds, set.ActualDurationSeconds,
		set.AccuracyScore, set.CompletionStatus, set.Notes)

	return err
}

// GetUserSessions возвращает сессии пользователя
func (r *WorkoutRepository) GetUserSessions(userID string, limit int) ([]models.WorkoutSession, error) {
	var sessions []models.WorkoutSession
	query := `
		SELECT * FROM workout_sessions 
		WHERE user_id = $1 
		ORDER BY started_at DESC 
		LIMIT $2
	`
	err := r.db.Select(&sessions, query, userID, limit)
	return sessions, err
}

// GetSessionDetails возвращает детали сессии с упражнениями
func (r *WorkoutRepository) GetSessionDetails(sessionID string) (*models.WorkoutSession, []models.ExerciseSet, error) {
	var session models.WorkoutSession
	query := `SELECT * FROM workout_sessions WHERE id = $1`
	err := r.db.Get(&session, query, sessionID)
	if err != nil {
		return nil, nil, err
	}

	var sets []models.ExerciseSet
	query = `SELECT * FROM exercise_sets WHERE session_id = $1 ORDER BY started_at`
	err = r.db.Select(&sets, query, sessionID)
	if err != nil && err != sql.ErrNoRows {
		return &session, nil, err
	}

	return &session, sets, nil
}

// GetActiveSession возвращает активную сессию пользователя
func (r *WorkoutRepository) GetActiveSession(userID string) (*models.WorkoutSession, error) {
	var session models.WorkoutSession
	query := `
		SELECT * FROM workout_sessions 
		WHERE user_id = $1 AND status = 'in_progress'
		ORDER BY started_at DESC
		LIMIT 1
	`
	err := r.db.Get(&session, query, userID)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &session, nil
}
