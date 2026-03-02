package com.example.lfk.api

import com.example.lfk.api.UserDeserializer
import com.example.lfk.models.*
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

    @POST("api/exercise/reset")
    suspend fun resetExercise(
        @Header("Authorization") token: String,
        @Body request: Map<String, String>
    ): Map<String, Any>
}

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/" // Для эмулятора Android

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Создаем Gson с кастомным десериализатором
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
}