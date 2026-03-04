package com.example.lfk.api

import android.util.Base64
import android.util.Log
import com.example.lfk.models.WebSocketResponse
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

class WebSocketManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val _messages = MutableSharedFlow<WebSocketResponse>(extraBufferCapacity = 50)
    val messages = _messages.asSharedFlow()

    private var isConnected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun connect(url: String) {
        disconnect()

        Log.d("WebSocketManager", "Connecting to: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                Log.d("WebSocketManager", "✅ WebSocket opened")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    Log.d("WebSocketManager", "📩 Received: ${text.length} chars")
                    val response = gson.fromJson(text, WebSocketResponse::class.java)
                    scope.launch {
                        _messages.emit(response)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketManager", "Error parsing: ${e.message}")
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "Closing: $reason")
                isConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "Closed: $reason")
                isConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "Failure: ${t.message}")
                t.printStackTrace()
                isConnected = false
            }
        })
    }

    fun sendFrame(frameBytes: ByteArray, exerciseType: String) {
        if (!isConnected) {
            Log.e("WebSocketManager", "❌ НЕТ ПОДКЛЮЧЕНИЯ!")
            return
        }

        try {
            val base64Frame = Base64.encodeToString(frameBytes, Base64.NO_WRAP)
            val message = mapOf(
                "frame" to base64Frame,
                "exercise_type" to exerciseType
            )
            val jsonMessage = gson.toJson(message)

            Log.d("WebSocketManager", "📤 Отправка: ${frameBytes.size} байт")

            webSocket?.send(jsonMessage)

        } catch (e: Exception) {
            Log.e("WebSocketManager", "❌ Ошибка отправки: ${e.message}")
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "Closing")
        webSocket = null
        isConnected = false
    }

    fun isConnected(): Boolean = isConnected
}