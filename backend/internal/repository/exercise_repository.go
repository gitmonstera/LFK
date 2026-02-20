package repository

import (
	"lfk-backend/internal/models"

	"github.com/jmoiron/sqlx"
)

type ExerciseRepository struct {
	db *sqlx.DB
}

func NewExerciseRepository(db *sqlx.DB) *ExerciseRepository {
	return &ExerciseRepository{db: db}
}

// GetAll возвращает все упражнения
func (r *ExerciseRepository) GetAll() ([]models.Exercise, error) {
	var exercises []models.Exercise
	query := `SELECT * FROM exercises WHERE is_active = true ORDER BY name`
	err := r.db.Select(&exercises, query)
	return exercises, err
}

// GetByID возвращает упражнение по ID
func (r *ExerciseRepository) GetByID(id string) (*models.Exercise, error) {
	var exercise models.Exercise
	query := `SELECT * FROM exercises WHERE id = $1 AND is_active = true`
	err := r.db.Get(&exercise, query, id)
	if err != nil {
		return nil, err
	}
	return &exercise, nil
}

// GetByCategory возвращает упражнения по категории
func (r *ExerciseRepository) GetByCategory(categoryID int) ([]models.Exercise, error) {
	var exercises []models.Exercise
	query := `SELECT * FROM exercises WHERE category_id = $1 AND is_active = true ORDER BY name`
	err := r.db.Select(&exercises, query, categoryID)
	return exercises, err
}
