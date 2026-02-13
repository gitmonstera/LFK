package models

import (
	"github.com/golang-jwt/jwt/v5"
)

// LoginRequest запрос на вход
type LoginRequest struct {
	Email    string `json:"email" binding:"required,email"`
	Password string `json:"password" binding:"required"`
}

// LoginResponse ответ с токеном
type LoginResponse struct {
	Token string `json:"token"`
	User  User   `json:"user"`
}

// Claims для JWT токена
type Claims struct {
	UserID   string `json:"user_id"`
	Username string `json:"username"`
	Email    string `json:"email"`
	jwt.RegisteredClaims
}
