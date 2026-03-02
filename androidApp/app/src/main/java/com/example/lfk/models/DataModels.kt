package com.example.lfk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модели данных для работы с API и WebSocket
 * Используем @Parcelize для передачи данных между компонентами
 */

/**
 * Пользователь системы
 */
@Parcelize
data class User(
    val id: String,
    val username: String,
    val email: String,
    val first_name: String?,
    val last_name: String?,
    val created_at: String
) : Parcelable

/**
 * Ответ сервера при авторизации/регистрации
 */
@Parcelize
data class AuthResponse(
    val token: String,
    val user: User
) : Parcelable

/**
 * Запрос на вход
 */
@Parcelize
data class LoginRequest(
    val email: String,
    val password: String
) : Parcelable

/**
 * Запрос на регистрацию
 */
@Parcelize
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val first_name: String? = null,
    val last_name: String? = null
) : Parcelable

/**
 * Ответ при начале тренировки
 */
@Parcelize
data class WorkoutStartResponse(
    val id: String
) : Parcelable

/**
 * Запрос на сохранение выполненного упражнения
 */
@Parcelize
data class ExerciseSetRequest(
    val session_id: String,
    val exercise_id: String,
    val actual_repetitions: Int,
    val actual_duration: Int,
    val accuracy_score: Double
) : Parcelable

/**
 * Сообщение для WebSocket (отправка кадра)
 */
@Parcelize
data class WebSocketFrame(
    val frame: String,  // Base64 encoded JPEG
    val exercise_type: String
) : Parcelable

/**
 * Ответ от WebSocket (обработанный кадр)
 */
@Parcelize
data class WebSocketResponse(
    val message: String,
    val hand_detected: Boolean,
    val raised_fingers: Int?,
    val finger_states: List<Boolean>?,
    val processed_frame: String?,  // Base64 encoded JPEG with landmarks
    val structured: StructuredData?,
    val timestamp: String
) : Parcelable

/**
 * Структурированные данные для упражнения "Кулак-ладонь"
 */
@Parcelize
data class StructuredData(
    val state: String?,          // Текущее состояние: waiting_fist, holding_fist, waiting_palm, holding_palm
    val current_cycle: Int?,     // Текущий цикл
    val total_cycles: Int?,      // Всего циклов
    val countdown: Int?,         // Обратный отсчет (для holding состояний)
    val progress_percent: Float?, // Прогресс выполнения (0-100)
    val completed: Boolean?,     // Завершено ли упражнение
    val auto_reset: Boolean?     // Автосброс после завершения
) : Parcelable
