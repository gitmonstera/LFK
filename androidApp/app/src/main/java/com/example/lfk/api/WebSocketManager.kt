package com.example.lfk.api

import android.util.Base64
import com.example.lfk.models.WebSocketResponse
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Менеджер WebSocket соединений для реального времени
 * Отвечает за отправку кадров и получение обработанных изображений
 */
class WebSocketManager {
    // HTTP клиент для WebSocket
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // Поток сообщений от сервера
    private val _messages = MutableSharedFlow<WebSocketResponse>()
    val messages = _messages.asSharedFlow()

    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Подключение к WebSocket серверу
     */
    fun connect(url: String, token: String) {
        val request = Request.Builder()
            .url("$url?token=$token")  // Токен в query параметре
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                println("WebSocket opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    // Парсим JSON ответ
                    val response = gson.fromJson(text, WebSocketResponse::class.java)
                    scope.launch {
                        _messages.emit(response)  // Отправляем в Flow
                    }
                } catch (e: Exception) {
                    println("Error parsing message: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                webSocket.close(1000, null)
                println("WebSocket closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                println("WebSocket failure: ${t.message}")
            }
        })
    }

    /**
     * Отправка кадра на сервер
     */
    fun sendFrame(frameBytes: ByteArray, exerciseType: String) {
        if (!isConnected) return

        // Конвертируем JPEG в Base64
        val base64Frame = Base64.encodeToString(frameBytes, Base64.DEFAULT)
        val message = mapOf(
            "frame" to base64Frame,
            "exercise_type" to exerciseType
        )

        webSocket?.send(gson.toJson(message))
    }

    /**
     * Отключение от WebSocket
     */
    fun disconnect() {
        webSocket?.close(1000, "Closing connection")
        webSocket = null
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected
}