package com.example.lfk.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lfk.api.ApiClient
import com.example.lfk.api.WebSocketManager
import com.example.lfk.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * ViewModel для выполнения упражнений
 * Управляет WebSocket соединением, камерой и прогрессом упражнений
 */
class ExerciseViewModel : ViewModel() {
    private val apiService = ApiClient.instance
    private val webSocketManager = WebSocketManager()

    // === LiveData для обновления UI ===

    // Обработанное изображение от сервера
    private val _processedImage = MutableLiveData<Bitmap?>()
    val processedImage: LiveData<Bitmap?> = _processedImage

    // Обнаружена ли рука
    private val _handDetected = MutableLiveData(false)
    val handDetected: LiveData<Boolean> = _handDetected

    // Количество поднятых пальцев
    private val _raisedFingers = MutableLiveData(0)
    val raisedFingers: LiveData<Int> = _raisedFingers

    // Состояние каждого пальца
    private val _fingerStates = MutableLiveData<List<Boolean>>(listOf(false, false, false, false, false))
    val fingerStates: LiveData<List<Boolean>> = _fingerStates

    // Текстовое сообщение от сервера
    private val _message = MutableLiveData("")
    val message: LiveData<String> = _message

    // Структурированные данные для упражнения "Кулак-ладонь"
    private val _structuredData = MutableLiveData<StructuredData?>()
    val structuredData: LiveData<StructuredData?> = _structuredData

    // ID текущей тренировки
    private val _currentSessionId = MutableLiveData<String?>()
    val currentSessionId: LiveData<String?> = _currentSessionId

    // Количество выполненных подходов
    private val _setsCompleted = MutableLiveData(0)
    val setsCompleted: LiveData<Int> = _setsCompleted

    // Всего циклов в упражнении
    private val _totalCycles = MutableLiveData(5)
    val totalCycles: LiveData<Int> = _totalCycles

    // Завершено ли упражнение
    private val _exerciseCompleted = MutableLiveData(false)
    val exerciseCompleted: LiveData<Boolean> = _exerciseCompleted

    // Состояние загрузки
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Ошибка
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Внутренние переменные для отслеживания прогресса
    private var lastCycle = -1
    private val statsSavedForCycle = mutableSetOf<Int>()
    private var receiveJob: Job? = null

    init {
        // Подписываемся на сообщения WebSocket при создании ViewModel
        receiveJob = webSocketManager.messages
            .onEach { response ->
                handleWebSocketResponse(response)
            }
            .launchIn(viewModelScope)
    }

    /**
     * Подключение к WebSocket для выполнения упражнения
     */
    fun connectToExercise(exerciseType: String, token: String, url: String) {
        webSocketManager.connect(url, token)
    }

    /**
     * Начало новой тренировки
     */
    fun startWorkout(token: String, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = apiService.startWorkout("Bearer $token")
                _currentSessionId.value = response.id
                onSuccess(response.id)
                Log.d("ExerciseViewModel", "Workout started: ${response.id}")
            } catch (e: Exception) {
                _error.value = "Ошибка начала тренировки: ${e.message}"
                Log.e("ExerciseViewModel", "Start workout error", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Отправка кадра с камеры на сервер
     */
    fun sendFrame(frameBytes: ByteArray, exerciseType: String) {
        webSocketManager.sendFrame(frameBytes, exerciseType)
    }

    /**
     * Добавление выполненного подхода
     */
    fun addExerciseSet(
        token: String,
        sessionId: String,
        exerciseId: String,
        repetitions: Int,
        duration: Int,
        accuracy: Double,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val request = ExerciseSetRequest(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    actual_repetitions = repetitions,
                    actual_duration = duration,
                    accuracy_score = accuracy
                )

                apiService.addExerciseSet("Bearer $token", request)
                Log.d("ExerciseViewModel", "Exercise set added")
                onSuccess?.invoke()
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Add exercise set error", e)
            }
        }
    }

    /**
     * Завершение тренировки
     */
    fun endWorkout(token: String, sessionId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                apiService.endWorkout("Bearer $token", mapOf("session_id" to sessionId))
                Log.d("ExerciseViewModel", "Workout ended")
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "End workout error", e)
            }
        }
    }

    /**
     * Сброс упражнения на сервере
     */
    fun resetExercise(token: String, exerciseType: String) {
        viewModelScope.launch {
            try {
                apiService.resetExercise("Bearer $token", mapOf("exercise_type" to exerciseType))
                Log.d("ExerciseViewModel", "Exercise reset requested")
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Reset exercise error", e)
            }
        }
    }

    /**
     * Обработка ответа от WebSocket
     */
    private fun handleWebSocketResponse(response: WebSocketResponse) {
        // Обновляем базовые значения
        _handDetected.value = response.hand_detected
        _raisedFingers.value = response.raised_fingers ?: 0
        _fingerStates.value = response.finger_states ?: listOf(false, false, false, false, false)
        _message.value = response.message ?: ""

        // Декодируем обработанное изображение
        response.processed_frame?.let { base64Frame ->
            try {
                val decodedBytes = Base64.decode(base64Frame, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                _processedImage.value = bitmap
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Error decoding image", e)
            }
        }

        // Обрабатываем структурированные данные для "Кулак-ладонь"
        response.structured?.let { structured ->
            _structuredData.value = structured

            structured.total_cycles?.let { _totalCycles.value = it }

            // Отслеживаем завершение циклов
            val currentCycle = structured.current_cycle ?: 0
            val completed = structured.completed ?: false

            // Если цикл увеличился - значит предыдущий завершен
            if (currentCycle > lastCycle && lastCycle >= 0) {
                val completedCycle = lastCycle
                if (completedCycle !in statsSavedForCycle && completedCycle > 0) {
                    // Цикл завершен, но статистика будет сохранена в ExerciseScreen
                }
            }

            // Если упражнение полностью завершено
            if (completed && !(_exerciseCompleted.value == true)) {
                _exerciseCompleted.value = true
            }

            lastCycle = currentCycle
        }
    }

    /**
     * Сброс состояния ViewModel
     */
    fun resetState() {
        _processedImage.value = null
        _handDetected.value = false
        _raisedFingers.value = 0
        _fingerStates.value = listOf(false, false, false, false, false)
        _message.value = ""
        _structuredData.value = null
        _setsCompleted.value = 0
        _exerciseCompleted.value = false
        lastCycle = -1
        statsSavedForCycle.clear()
    }

    /**
     * Отключение от WebSocket
     */
    fun disconnect() {
        webSocketManager.disconnect()
        receiveJob?.cancel()
        resetState()
    }

    fun isConnected(): Boolean = webSocketManager.isConnected()

    fun incrementSetsCompleted() {
        _setsCompleted.value = (_setsCompleted.value ?: 0) + 1
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}