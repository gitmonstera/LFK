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

// GetExerciseList возвращает список упражнений для API
func (r *ExerciseRepository) GetExerciseList() ([]models.ExerciseInfo, error) {
	var exercises []models.ExerciseInfo

	query := `
        SELECT 
            e.id,
            e.name,
            e.description,
            COALESCE(c.name, '') as category,
            COALESCE(e.category_id, 0) as category_id,
            COALESCE(c.icon, '') as category_icon,
            COALESCE(c.color, '') as category_color,
            COALESCE(e.difficulty_level, 0) as difficulty_level,
            COALESCE(e.target_muscles, '{}') as target_muscles,
            COALESCE(e.instructions, '{}') as instructions,
            COALESCE(e.duration_seconds, 0) as duration_seconds,
            e.image_url,
            e.video_url,
            COALESCE(e.applicable_codes, '{}') as applicable_codes   -- новое поле
        FROM exercises e
        LEFT JOIN exercise_categories c ON e.category_id = c.id
        WHERE e.is_active = true
        ORDER BY e.name
    `

	err := r.db.Select(&exercises, query)
	if err != nil {
		return nil, err
	}

	return exercises, nil
}

// GetAllCategories возвращает все категории
func (r *ExerciseRepository) GetAllCategories() ([]models.Category, error) {
	var categories []models.Category
	query := `SELECT * FROM exercise_categories ORDER BY name`
	err := r.db.Select(&categories, query)
	return categories, err
}

// GetCategoryByID возвращает категорию по ID
func (r *ExerciseRepository) GetCategoryByID(id int) (*models.Category, error) {
	var category models.Category
	query := `SELECT * FROM exercise_categories WHERE id = $1`
	err := r.db.Get(&category, query, id)
	if err != nil {
		return nil, err
	}
	return &category, nil
}
