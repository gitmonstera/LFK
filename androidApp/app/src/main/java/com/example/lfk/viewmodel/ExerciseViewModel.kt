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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ExerciseViewModel : ViewModel() {
    private val apiService = ApiClient.instance
    private val webSocketManager = WebSocketManager()

    private val _processedImage = MutableLiveData<Bitmap?>()
    val processedImage: LiveData<Bitmap?> = _processedImage

    private val _handDetected = MutableLiveData(false)
    val handDetected: LiveData<Boolean> = _handDetected

    private val _raisedFingers = MutableLiveData(0)
    val raisedFingers: LiveData<Int> = _raisedFingers

    private val _fingerStates = MutableLiveData<List<Boolean>>(listOf(false, false, false, false, false))
    val fingerStates: LiveData<List<Boolean>> = _fingerStates

    private val _message = MutableLiveData("")
    val message: LiveData<String> = _message

    private val _structuredData = MutableLiveData<StructuredData?>()
    val structuredData: LiveData<StructuredData?> = _structuredData

    private val _currentSessionId = MutableLiveData<String?>()
    val currentSessionId: LiveData<String?> = _currentSessionId

    private val _setsCompleted = MutableLiveData(0)
    val setsCompleted: LiveData<Int> = _setsCompleted

    private val _totalCycles = MutableLiveData(5)
    val totalCycles: LiveData<Int> = _totalCycles

    private val _exerciseCompleted = MutableLiveData(false)
    val exerciseCompleted: LiveData<Boolean> = _exerciseCompleted

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _exerciseState = MutableLiveData<Map<String, Any>?>()
    val exerciseState: LiveData<Map<String, Any>?> = _exerciseState

    // Flow для UI
    private val _webSocketMessages = MutableSharedFlow<WebSocketResponse>()
    val webSocketMessages = _webSocketMessages.asSharedFlow()

    private var lastCycle = -1
    private val statsSavedForCycle = mutableSetOf<Int>()
    private var receiveJob: Job? = null

    // Больше никакой подписки в init!
    init {
        // Пусто!
    }

    fun connectToExercise(exerciseType: String, url: String) {
        Log.d("ExerciseViewModel", "Connecting to WebSocket: $url")

        // Отменяем старую подписку
        receiveJob?.cancel()

        // Подключаемся
        webSocketManager.connect(url)

        // СОЗДАЕМ НОВУЮ ПОДПИСКУ для этого подключения
        receiveJob = webSocketManager.messages
            .onEach { response ->
                handleWebSocketResponse(response)
                // Также отправляем в отдельный Flow для UI
                _webSocketMessages.emit(response)
            }
            .launchIn(viewModelScope)

        Log.d("ExerciseViewModel", "New subscription created")
    }

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

    fun checkExerciseState(token: String, exerciseType: String) {
        viewModelScope.launch {
            try {
                val response = apiService.checkExerciseState("Bearer $token", exerciseType)
                _exerciseState.value = response
                Log.d("ExerciseViewModel", "Exercise state: $response")
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Error checking exercise state", e)
            }
        }
    }

    fun resetExercise(token: String, exerciseType: String) {
        viewModelScope.launch {
            try {
                val response = apiService.resetExercise(
                    "Bearer $token",
                    mapOf("exercise_type" to exerciseType)
                )
                Log.d("ExerciseViewModel", "Exercise reset: $response")
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Error resetting exercise", e)
            }
        }
    }

    fun sendFrame(frameBytes: ByteArray, exerciseType: String) {
        Log.d("ExerciseViewModel", "📤 sendFrame вызван: ${frameBytes.size} байт")
        webSocketManager.sendFrame(frameBytes, exerciseType)
    }

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

    private fun handleWebSocketResponse(response: WebSocketResponse) {
        Log.d("ExerciseViewModel", "Handling response: hand=${response.hand_detected}")

        _handDetected.value = response.hand_detected
        _raisedFingers.value = response.raised_fingers ?: 0
        _fingerStates.value = response.finger_states ?: listOf(false, false, false, false, false)
        _message.value = response.message ?: ""

        // Обрабатываем изображение
        response.processed_frame?.let { base64Frame ->
            try {
                val decodedBytes = Base64.decode(base64Frame, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                _processedImage.value = bitmap
            } catch (e: Exception) {
                Log.e("ExerciseViewModel", "Error decoding", e)
            }
        }

        response.structured?.let {
            _structuredData.value = it
        }
    }

    fun resetState() {
        _processedImage.value = null
        _handDetected.value = false
        _raisedFingers.value = 0
        _fingerStates.value = listOf(false, false, false, false, false)
        _message.value = ""
        _structuredData.value = null
        _setsCompleted.value = 0
        _exerciseCompleted.value = false
        _exerciseState.value = null
        lastCycle = -1
        statsSavedForCycle.clear()
    }

    fun disconnect() {
        Log.d("ExerciseViewModel", "Disconnecting...")
        webSocketManager.disconnect()
        receiveJob?.cancel()
        receiveJob = null
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