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

/**
 * Общая статистика
 */
@Parcelize
data class OverallStats(
    val total_sessions: Int,
    val total_exercises: Int,
    val total_repetitions: Int,
    val total_duration: Int,
    val unique_exercises: Int,
    val current_streak: Int,
    val longest_streak: Int,
    val last_workout_at: String?
) : Parcelable

/**
 * Дневная статистика
 */
@Parcelize
data class DailyStats(
    val total_sessions: Int,
    val total_exercises: Int,
    val total_duration_seconds: Int,
    val calories_burned: Double,
    val completed: Boolean
) : Parcelable

/**
 * Статистика за неделю (массив дней)
 */
typealias WeeklyStats = List<DailyStatItem>

@Parcelize
data class DailyStatItem(
    val stat_date: String,
    val total_sessions: Int,
    val total_exercises: Int,
    val total_duration_seconds: Int,
    val completed: Boolean
) : Parcelable

/**
 * Статистика за месяц (сгруппированная по неделям)
 */
@Parcelize
data class MonthlyStats(
    val weeks: List<WeekStats>
) : Parcelable

@Parcelize
data class WeekStats(
    val week_number: Int,
    val sessions: Int,
    val exercises: Int,
    val duration: Int
) : Parcelable

/**
 * Статистика по упражнениям
 */
@Parcelize
data class ExerciseStatItem(
    val exercise_name: String,
    val total_sessions: Int,
    val total_repetitions: Int,
    val total_duration: Int,
    val best_accuracy: Double?,
    val avg_accuracy: Double?,
    val last_performed_at: String?
) : Parcelable

/**
 * История тренировок
 */
@Parcelize
data class WorkoutHistoryItem(
    val started_at: String,
    val total_exercises: Int,
    val total_reps: Int,
    val total_duration: Int,
    val avg_accuracy: Double,
    val exercises: List<HistoryExercise>
) : Parcelable

@Parcelize
data class HistoryExercise(
    val name: String,
    val repetitions: Int
) : Parcelable