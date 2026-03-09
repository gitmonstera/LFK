package com.example.lfk.api

import com.example.lfk.models.*
import com.example.lfk.api.UserDeserializer
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("api/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("api/profile")
    suspend fun getProfile(@Header("Authorization") token: String): User

    @POST("api/workout/start")
    suspend fun startWorkout(@Header("Authorization") token: String): WorkoutStartResponse

    @POST("api/workout/exercise")
    suspend fun addExerciseSet(
        @Header("Authorization") token: String,
        @Body request: ExerciseSetRequest
    ): Map<String, String>

    @POST("api/workout/end")
    suspend fun endWorkout(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Map<String, String>

    @GET("api/exercise_state")
    suspend fun checkExerciseState(
        @Header("Authorization") token: String,
        @Query("type") exerciseType: String
    ): Map<String, Any>

    @POST("api/exercise/reset")
    suspend fun resetExercise(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Map<String, Any>

    @GET("api/get_exercise_list")
    suspend fun getExerciseList(
        @Header("Authorization") token: String
    ): ExerciseListResponse

    @GET("api/stats/overall")
    suspend fun getOverallStats(
        @Header("Authorization") token: String
    ): OverallStats

    @GET("api/stats/daily")
    suspend fun getDailyStats(
        @Header("Authorization") token: String,
        @Query("date") date: String
    ): DailyStats

    @GET("api/stats/weekly")
    suspend fun getWeeklyStats(
        @Header("Authorization") token: String
    ): List<DailyStatItem>

    @GET("api/stats/monthly")
    suspend fun getMonthlyStats(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @GET("api/stats/exercises")
    suspend fun getExerciseStats(
        @Header("Authorization") token: String
    ): List<ExerciseStatItem>

    @GET("api/workout/history")
    suspend fun getWorkoutHistory(
        @Header("Authorization") token: String
    ): List<WorkoutHistoryItem>
}

object ApiClient {
    // IP вашего компьютера в локальной сети
    private const val BASE_URL = "http://172.20.10.3:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(User::class.java, UserDeserializer())
        .create()

    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    // Функция для получения WebSocket URL с токеном в query параметре
    fun getWebSocketUrl(exerciseType: String, token: String): String {
        val baseWsUrl = BASE_URL.replace("http://", "ws://")
        // Формируем URL: ws://192.168.0.143:8080/ws/exercise/fist-palm?token=...
        return "${baseWsUrl}ws/exercise/$exerciseType?token=$token"
    }
}