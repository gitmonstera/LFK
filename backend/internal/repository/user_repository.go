package repository

import (
	"database/sql"
	"fmt"
	"lfk-backend/internal/models"

	"github.com/jmoiron/sqlx"
)

type UserRepository struct {
	db *sqlx.DB
}

func NewUserRepository(db *sqlx.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) Create(user *models.User) error {
	// Добавили все поля, которые есть в таблице
	query := `
		INSERT INTO users (
			username, email, password_hash, 
			first_name, last_name, 
			birth_date, gender, height_cm, weight_kg,
			is_active, role,
			created_at, updated_at
		) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, NOW(), NOW())
		RETURNING id, created_at, updated_at
	`

	fmt.Printf("Executing query for user: %s, %s\n", user.Username, user.Email)

	// Выполняем запрос и получаем сгенерированный ID
	err := r.db.QueryRow(
		query,
		user.Username,
		user.Email,
		user.PasswordHash,
		user.FirstName,
		user.LastName,
		user.BirthDate, // NULL
		user.Gender,    // NULL
		user.HeightCm,  // NULL
		user.WeightKg,  // NULL
		true,           // is_active
		"user",         // role - значение по умолчанию
	).Scan(&user.ID, &user.CreatedAt, &user.UpdatedAt)

	if err != nil {
		fmt.Printf("Database error: %v\n", err)
		return err
	}

	fmt.Printf("User created successfully with ID: %s\n", user.ID)
	return nil
}

func (r *UserRepository) GetByEmail(email string) (*models.User, error) {
	var user models.User
	query := `SELECT * FROM users WHERE email = $1 AND is_active = true`
	err := r.db.Get(&user, query, email)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByUsername(username string) (*models.User, error) {
	var user models.User
	query := `SELECT * FROM users WHERE username = $1 AND is_active = true`
	err := r.db.Get(&user, query, username)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) GetByID(id string) (*models.User, error) {
	var user models.User
	query := `SELECT * FROM users WHERE id = $1 AND is_active = true`
	err := r.db.Get(&user, query, id)
	if err != nil {
		if err == sql.ErrNoRows {
			return nil, nil
		}
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) UpdateLastLogin(id string) error {
	query := `UPDATE users SET last_login = NOW() WHERE id = $1`
	_, err := r.db.Exec(query, id)
	return err
}

// Update обновляет данные пользователя
func (r *UserRepository) Update(user *models.User) error {
	query := `
		UPDATE users 
		SET first_name = $1, last_name = $2, birth_date = $3, 
		    gender = $4, height_cm = $5, weight_kg = $6, updated_at = NOW()
		WHERE id = $7
	`
	_, err := r.db.Exec(query,
		user.FirstName, user.LastName, user.BirthDate,
		user.Gender, user.HeightCm, user.WeightKg, user.ID)
	return err
}

// UpdatePassword обновляет пароль пользователя
func (r *UserRepository) UpdatePassword(id string, passwordHash string) error {
	query := `UPDATE users SET password_hash = $1, updated_at = NOW() WHERE id = $2`
	_, err := r.db.Exec(query, passwordHash, id)
	return err
}
