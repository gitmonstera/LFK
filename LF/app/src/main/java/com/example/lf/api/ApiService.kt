package com.example.lf.api

import retrofit2.Response
import retrofit2.http.*
import com.google.gson.annotations.SerializedName

// ==================== МОДЕЛИ ЗАПРОСОВ ====================
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val first_name: String? = null,
    val last_name: String? = null
)

data class ExerciseResultRequest(
    @SerializedName("session_id") val session_id: String,
    @SerializedName("exercise_id") val exercise_id: String,
    @SerializedName("actual_repetitions") val actual_repetitions: Int,
    @SerializedName("actual_duration") val actual_duration: Int,
    @SerializedName("accuracy_score") val accuracy_score: Float
)

// ==================== МОДЕЛИ ОТВЕТОВ ====================
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserResponse
)

data class UserResponse(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("email") val email: String,
    @SerializedName("first_name") val firstName: Map<String, Any>?,
    @SerializedName("last_name") val lastName: Map<String, Any>?,
    @SerializedName("created_at") val createdAt: String
) {
    fun getFirstName(): String? {
        return firstName?.get("String") as? String
    }

    fun getLastName(): String? {
        return lastName?.get("String") as? String
    }
}

data class ExerciseListResponse(
    @SerializedName("items") val items: List<ExerciseResponse>
)

data class ExerciseResponse(
    @SerializedName("exercise_id") val exerciseId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_icon") val categoryIcon: String?,
    @SerializedName("category_color") val categoryColor: String,
    @SerializedName("difficulty_level") val difficultyLevel: Int,
    @SerializedName("target_muscles") val targetMuscles: List<String>,
    @SerializedName("instructions") val instructions: List<String>,
    @SerializedName("duration_seconds") val durationSeconds: Int
)

data class WorkoutStartResponse(
    @SerializedName("id") val id: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("status") val status: String
)

data class OverallStatsResponse(
    @SerializedName("user_id") val userId: String,
    @SerializedName("total_sessions") val totalSessions: Int,
    @SerializedName("total_exercises") val totalExercises: Int,
    @SerializedName("total_repetitions") val totalRepetitions: Int,
    @SerializedName("total_duration") val totalDuration: Int,
    @SerializedName("unique_exercises") val uniqueExercises: Int,
    @SerializedName("current_streak") val currentStreak: Int,
    @SerializedName("longest_streak") val longestStreak: Int,
    @SerializedName("last_workout_at") val lastWorkoutAt: String?
)

data class DailyStatsResponse(
    @SerializedName("stat_date") val statDate: String,
    @SerializedName("total_sessions") val totalSessions: Int,
    @SerializedName("total_exercises") val totalExercises: Int,
    @SerializedName("total_duration_seconds") val totalDurationSeconds: Int,
    @SerializedName("calories_burned") val caloriesBurned: Float,
    @SerializedName("completed") val completed: Boolean
)

data class ExerciseStatResponse(
    @SerializedName("exercise_id") val exerciseId: String,
    @SerializedName("exercise_name") val exerciseName: String,
    @SerializedName("total_sessions") val totalSessions: Int,
    @SerializedName("total_repetitions") val totalRepetitions: Int,
    @SerializedName("total_duration") val totalDuration: Int,
    @SerializedName("best_accuracy") val bestAccuracy: Float?,
    @SerializedName("avg_accuracy") val avgAccuracy: Float?,
    @SerializedName("last_performed_at") val lastPerformedAt: String?
)

data class WorkoutHistoryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("started_at") val startedAt: String,
    @SerializedName("ended_at") val endedAt: String?,
    @SerializedName("total_exercises") val totalExercises: Int,
    @SerializedName("total_reps") val totalReps: Int,
    @SerializedName("total_duration") val totalDuration: Int,
    @SerializedName("avg_accuracy") val avgAccuracy: Float,
    @SerializedName("exercises") val exercises: List<WorkoutExerciseResponse>
)

data class WorkoutExerciseResponse(
    @SerializedName("name") val name: String,
    @SerializedName("repetitions") val repetitions: Int,
    @SerializedName("duration") val duration: Int,
    @SerializedName("accuracy") val accuracy: Float
)

// ==================== API ИНТЕРФЕЙС ====================
interface ApiService {

    @POST("api/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<AuthResponse>

    @POST("api/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<AuthResponse>

    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String
    ): Response<UserResponse>

    @GET("api/get_exercise_list")
    suspend fun getExerciseList(
        @Header("Authorization") token: String
    ): Response<ExerciseListResponse>

    @GET("api/exercise_state")
    suspend fun getExerciseState(
        @Header("Authorization") token: String,
        @Query("type") exerciseType: String
    ): Response<Map<String, Any>>

    @POST("api/exercise/reset")
    suspend fun resetExercise(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/workout/start")
    suspend fun startWorkout(
        @Header("Authorization") token: String
    ): Response<WorkoutStartResponse>

    @POST("api/workout/exercise")
    suspend fun addExerciseSet(
        @Header("Authorization") token: String,
        @Body request: ExerciseResultRequest
    ): Response<Map<String, String>>

    @POST("api/workout/end")
    suspend fun endWorkout(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/workout/history")
    suspend fun getWorkoutHistory(
        @Header("Authorization") token: String
    ): Response<List<WorkoutHistoryResponse>>

    @GET("api/stats/overall")
    suspend fun getOverallStats(
        @Header("Authorization") token: String
    ): Response<OverallStatsResponse>

    @GET("api/stats/daily")
    suspend fun getDailyStats(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): Response<DailyStatsResponse>

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(
        @Header("Authorization") token: String
    ): Response<List<DailyStatsResponse>>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(
        @Header("Authorization") token: String
    ): Response<List<DailyStatsResponse>>

    @GET("api/stats/exercises")
    suspend fun getExerciseStats(
        @Header("Authorization") token: String
    ): Response<List<ExerciseStatResponse>>
}