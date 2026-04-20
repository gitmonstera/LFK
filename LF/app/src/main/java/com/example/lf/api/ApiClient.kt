package com.example.lf.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Float::class.java, NullFloatDeserializer())
        .registerTypeAdapter(Float::class.javaObjectType, NullFloatDeserializer())
        .registerTypeAdapter(Int::class.javaObjectType, NullIntDeserializer())
        .registerTypeAdapter(String::class.java, NullStringDeserializer())
        .create()

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        println("🔵 OkHttp: $message")
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}