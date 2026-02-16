package com.example.lfk

import android.util.Base64
import android.util.Log
import com.example.lfk.model.FrameFeedback
import com.google.gson.Gson
import okhttp3.*
import java.util.concurrent.TimeUnit

// Переименовываем интерфейс, чтобы избежать конфликта
interface WebSocketCallback {
    fun onFeedback(feedback: FrameFeedback)
    fun onError(error: String)
}

class WebSocketManager(
    private val token: String,
    private val exerciseType: String
) {
    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var callback: WebSocketCallback? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun connect(callback: WebSocketCallback) {
        this.callback = callback

        val exercisePath = when (exerciseType) {
            "fist" -> "fist"
            "fist-index" -> "fist-index"
            "fist-palm" -> "fist-palm"
            else -> "fist"
        }

        val url = "ws://10.0.2.2:8080/ws/exercise/$exercisePath?token=$token"
        // val url = "ws://192.168.1.XXX:8080/ws/exercise/$exercisePath?token=$token" // для реального устройства

        val request = Request.Builder()
            .url(url)
            .build()

        // Используем полное имя okhttp3.WebSocketListener
        webSocket = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val feedback = gson.fromJson(text, FrameFeedback::class.java)
                    this@WebSocketManager.callback?.onFeedback(feedback)
                } catch (e: Exception) {
                    Log.e("WebSocket", "Parse error: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
                this@WebSocketManager.callback?.onError(t.message ?: "Unknown error")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closed: $reason")
            }
        })
    }

    fun sendFrame(frameBase64: String) {
        val message = mapOf(
            "frame" to frameBase64,
            "exercise_type" to exerciseType
        )
        val json = gson.toJson(message)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        client.dispatcher.executorService.shutdown()
    }
}