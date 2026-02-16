package com.example.lfk

import com.example.lfk.model.*
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("/api/login")
    suspend fun login(@Body request: LoginRequest): retrofit2.Response<LoginResponse>

    @GET("/api/exercises")
    suspend fun getExercises(@Header("Authorization") token: String): retrofit2.Response<List<Exercise>>
}

class ApiClient {
    private val baseUrl = "http://10.0.2.2:8080" // для эмулятора
    // private val baseUrl = "http://192.168.1.XXX:8080" // для реального устройства

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val api: ApiService = retrofit.create(ApiService::class.java)

    var authToken: String? = null
}