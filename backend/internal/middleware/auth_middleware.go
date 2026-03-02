package middleware

import (
	"lfk-backend/internal/auth"
	"log"
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
)

func AuthMiddleware(jwtManager *auth.JWTManager) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Отладка - печатаем все заголовки и параметры
		log.Println("=== AUTH MIDDLEWARE DEBUG ===")
		log.Printf("Full URL: %s", c.Request.URL.String())
		log.Printf("Path: %s", c.Request.URL.Path)
		log.Printf("Method: %s", c.Request.Method)

		// Проверяем query параметр token
		token := c.Query("token")
		log.Printf("Token from query: '%s'", token)

		// Проверяем заголовок Authorization
		authHeader := c.GetHeader("Authorization")
		log.Printf("Authorization header: '%s'", authHeader)

		var tokenString string

		if token != "" {
			// Из query параметра
			tokenString = token
			log.Println("Using token from query parameter")
		} else if authHeader != "" {
			// Из заголовка
			parts := strings.Split(authHeader, " ")
			if len(parts) == 2 && strings.ToLower(parts[0]) == "bearer" {
				tokenString = parts[1]
				log.Println("Using token from Authorization header")
			} else {
				log.Println("Invalid Authorization header format")
			}
		} else {
			log.Println("No token found in query or header")
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Authorization token required",
			})
			c.Abort()
			return
		}

		if tokenString == "" {
			log.Println("Token string is empty")
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Invalid token format",
			})
			c.Abort()
			return
		}

		log.Printf("Token string (first 20 chars): %s...", tokenString[:20])

		// Проверяем токен
		claims, err := jwtManager.VerifyToken(tokenString)
		if err != nil {
			log.Printf("Token verification failed: %v", err)
			c.JSON(http.StatusUnauthorized, gin.H{
				"error": "Invalid or expired token",
			})
			c.Abort()
			return
		}

		log.Printf("Token verified successfully for user: %s", claims.Username)

		// Сохраняем информацию о пользователе
		c.Set("user_id", claims.UserID)
		c.Set("username", claims.Username)
		c.Set("email", claims.Email)

		c.Next()
	}
}
