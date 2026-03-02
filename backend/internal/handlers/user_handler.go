package handlers

import (
	"database/sql"
	"lfk-backend/internal/auth"
	"lfk-backend/internal/models"
	"lfk-backend/internal/repository"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
	"golang.org/x/crypto/bcrypt"
)

type UserHandler struct {
	userRepo   *repository.UserRepository
	jwtManager *auth.JWTManager
}

func NewUserHandler(userRepo *repository.UserRepository, jwtManager *auth.JWTManager) *UserHandler {
	return &UserHandler{
		userRepo:   userRepo,
		jwtManager: jwtManager,
	}
}

// Register - регистрация нового пользователя
func (h *UserHandler) Register(c *gin.Context) {
	var req models.RegisterRequest

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error":   "Invalid request format",
			"details": err.Error(),
		})
		return
	}

	// Проверяем email
	existingUser, _ := h.userRepo.GetByEmail(req.Email)
	if existingUser != nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": "Email already registered",
		})
		return
	}

	// Проверяем username
	existingByUsername, _ := h.userRepo.GetByUsername(req.Username)
	if existingByUsername != nil {
		c.JSON(http.StatusConflict, gin.H{
			"error": "Username already taken",
		})
		return
	}

	// Хешируем пароль
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(req.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to process password",
		})
		return
	}

	// Создаем пользователя
	user := &models.User{
		ID:           uuid.New().String(),
		Username:     req.Username,
		Email:        req.Email,
		PasswordHash: string(hashedPassword),
		FirstName:    toNullString(req.FirstName),
		LastName:     toNullString(req.LastName),
		IsActive:     true,
		Role:         "user",
	}

	if err := h.userRepo.Create(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to create user",
		})
		return
	}

	// Генерируем токен
	token, err := h.jwtManager.GenerateToken(user)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to generate token",
		})
		return
	}

	c.JSON(http.StatusCreated, models.LoginResponse{
		Token: token,
		User:  *user,
	})
}

// Login - вход пользователя
func (h *UserHandler) Login(c *gin.Context) {
	var req models.LoginRequest

	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"error":   "Invalid request format",
			"details": err.Error(),
		})
		return
	}

	user, err := h.userRepo.GetByEmail(req.Email)
	if err != nil || user == nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "Invalid email or password",
		})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "Invalid email or password",
		})
		return
	}

	_ = h.userRepo.UpdateLastLogin(user.ID)

	token, err := h.jwtManager.GenerateToken(user)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Failed to generate token",
		})
		return
	}

	c.JSON(http.StatusOK, models.LoginResponse{
		Token: token,
		User:  *user,
	})
}

// GetProfile - получение профиля пользователя
func (h *UserHandler) GetProfile(c *gin.Context) {
	userID := c.GetString("user_id")

	user, err := h.userRepo.GetByID(userID)
	if err != nil || user == nil {
		c.JSON(http.StatusNotFound, gin.H{
			"error": "User not found",
		})
		return
	}

	c.JSON(http.StatusOK, user)
}

// UpdateProfile - обновление профиля
type UpdateProfileRequest struct {
	FirstName string   `json:"first_name"`
	LastName  string   `json:"last_name"`
	BirthDate *string  `json:"birth_date"`
	Gender    string   `json:"gender"`
	HeightCm  *int     `json:"height_cm"`
	WeightKg  *float64 `json:"weight_kg"`
}

func (h *UserHandler) UpdateProfile(c *gin.Context) {
	userID := c.GetString("user_id")

	var req UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	user, err := h.userRepo.GetByID(userID)
	if err != nil || user == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}

	// Обновляем поля
	if req.FirstName != "" {
		user.FirstName = sql.NullString{String: req.FirstName, Valid: true}
	}
	if req.LastName != "" {
		user.LastName = sql.NullString{String: req.LastName, Valid: true}
	}
	if req.BirthDate != nil {
		if t, err := time.Parse("2006-01-02", *req.BirthDate); err == nil {
			user.BirthDate = sql.NullTime{Time: t, Valid: true}
		}
	}
	if req.Gender != "" {
		user.Gender = sql.NullString{String: req.Gender, Valid: true}
	}
	if req.HeightCm != nil {
		user.HeightCm = sql.NullInt64{Int64: int64(*req.HeightCm), Valid: true}
	}
	if req.WeightKg != nil {
		user.WeightKg = sql.NullFloat64{Float64: *req.WeightKg, Valid: true}
	}

	if err := h.userRepo.Update(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update profile"})
		return
	}

	c.JSON(http.StatusOK, user)
}

// ChangePassword - смена пароля
type ChangePasswordRequest struct {
	OldPassword string `json:"old_password" binding:"required"`
	NewPassword string `json:"new_password" binding:"required,min=6"`
}

func (h *UserHandler) ChangePassword(c *gin.Context) {
	userID := c.GetString("user_id")

	var req ChangePasswordRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	user, err := h.userRepo.GetByID(userID)
	if err != nil || user == nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "User not found"})
		return
	}

	// Проверяем старый пароль
	if err := bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(req.OldPassword)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid old password"})
		return
	}

	// Хешируем новый пароль
	newHash, err := bcrypt.GenerateFromPassword([]byte(req.NewPassword), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to process new password"})
		return
	}

	if err := h.userRepo.UpdatePassword(userID, string(newHash)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update password"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Password updated successfully"})
}

// CheckUser - проверяет существование пользователя
func (h *UserHandler) CheckUser(c *gin.Context) {
	email := c.Query("email")
	username := c.Query("username")

	if email == "" && username == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Either email or username is required",
		})
		return
	}

	if email != "" {
		user, _ := h.userRepo.GetByEmail(email)
		if user != nil {
			c.JSON(http.StatusOK, gin.H{
				"registered": true,
				"exists":     true,
				"method":     "email",
			})
			return
		}
	}

	if username != "" {
		user, _ := h.userRepo.GetByUsername(username)
		if user != nil {
			c.JSON(http.StatusOK, gin.H{
				"registered": true,
				"exists":     true,
				"method":     "username",
			})
			return
		}
	}

	c.JSON(http.StatusOK, gin.H{
		"registered": false,
		"exists":     false,
	})
}

// CheckEmail - проверяет email
func (h *UserHandler) CheckEmail(c *gin.Context) {
	email := c.Query("email")

	if email == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Email is required",
		})
		return
	}

	user, _ := h.userRepo.GetByEmail(email)
	if user == nil {
		c.JSON(http.StatusOK, gin.H{
			"registered": false,
			"available":  true,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"registered": true,
		"available":  false,
	})
}

// CheckUsername - проверяет username
func (h *UserHandler) CheckUsername(c *gin.Context) {
	username := c.Query("username")

	if username == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Username is required",
		})
		return
	}

	user, _ := h.userRepo.GetByUsername(username)
	if user == nil {
		c.JSON(http.StatusOK, gin.H{
			"registered": false,
			"available":  true,
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"registered": true,
		"available":  false,
	})
}

// Вспомогательная функция
func toNullString(s string) sql.NullString {
	if s == "" {
		return sql.NullString{Valid: false}
	}
	return sql.NullString{String: s, Valid: true}
}
