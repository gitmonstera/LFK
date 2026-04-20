package com.example.lf.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lf.api.ApiClient
import com.example.lf.api.ExerciseResponse
import com.example.lf.api.ExerciseResultRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ExerciseProgress(
    val state: String = "waiting",
    val currentCycle: Int = 0,
    val totalCycles: Int = 5,
    val progressPercent: Int = 0,
    val countdown: Int? = null,
    val message: String = "",
    val completed: Boolean = false
)

data class ExerciseState(
    val isLoading: Boolean = false,
    val exercises: List<ExerciseResponse> = emptyList(),
    val currentSessionId: String? = null,
    val currentProgress: ExerciseProgress = ExerciseProgress(),
    val error: String? = null
)

class ExerciseViewModel : ViewModel() {

    private val _exerciseState = MutableStateFlow(ExerciseState())
    val exerciseState: StateFlow<ExerciseState> = _exerciseState.asStateFlow()

    private val _exercisesList = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val exercisesList: StateFlow<List<ExerciseResponse>> = _exercisesList.asStateFlow()

    private val _exercisesState = MutableStateFlow<List<ExerciseResponse>>(emptyList())
    val exercisesState: StateFlow<List<ExerciseResponse>> = _exercisesState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    fun setAuthToken(token: String) {
        _authToken.value = token
    }

    fun loadExercises(token: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val response = ApiClient.apiService.getExerciseList("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    val exercises = response.body()!!.items
                    _exercisesList.value = exercises
                    _exercisesState.value = exercises
                    _exerciseState.value = _exerciseState.value.copy(
                        isLoading = false,
                        exercises = exercises,
                        error = null
                    )
                } else {
                    _exerciseState.value = ExerciseState(
                        isLoading = false,
                        error = response.errorBody()?.string() ?: "Ошибка загрузки"
                    )
                }
            } catch (e: Exception) {
                _exerciseState.value = ExerciseState(
                    isLoading = false,
                    error = e.message ?: "Ошибка соединения"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getExerciseById(id: String): ExerciseResponse? {
        return _exercisesList.value.find { it.exerciseId == id }
    }

    fun startWorkout(token: String) {
        viewModelScope.launch {
            _exerciseState.value = _exerciseState.value.copy(isLoading = true)

            try {
                val response = ApiClient.apiService.startWorkout("Bearer $token")

                if (response.isSuccessful && response.body() != null) {
                    _exerciseState.value = _exerciseState.value.copy(
                        isLoading = false,
                        currentSessionId = response.body()!!.id,
                        error = null
                    )
                } else {
                    _exerciseState.value = ExerciseState(
                        isLoading = false,
                        error = response.errorBody()?.string() ?: "Ошибка начала тренировки"
                    )
                }
            } catch (e: Exception) {
                _exerciseState.value = ExerciseState(
                    isLoading = false,
                    error = e.message ?: "Ошибка соединения"
                )
            }
        }
    }

    fun saveExerciseResult(token: String, sessionId: String, exerciseId: String, repetitions: Int, duration: Int, accuracy: Float) {
        viewModelScope.launch {
            try {
                val request = ExerciseResultRequest(
                    session_id = sessionId,
                    exercise_id = exerciseId,
                    actual_repetitions = repetitions,
                    actual_duration = duration,
                    accuracy_score = accuracy
                )

                val response = ApiClient.apiService.addExerciseSet("Bearer $token", request)

                if (response.isSuccessful) {
                    println("Результат сохранен")
                } else {
                    println("Ошибка сохранения: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                println("Ошибка: ${e.message}")
            }
        }
    }

    fun endWorkout(token: String, sessionId: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.endWorkout(
                    "Bearer $token",
                    mapOf("session_id" to sessionId)
                )

                if (response.isSuccessful) {
                    _exerciseState.value = _exerciseState.value.copy(currentSessionId = null)
                }
            } catch (e: Exception) {
                println("Ошибка завершения: ${e.message}")
            }
        }
    }

    fun updateProgress(progress: ExerciseProgress) {
        _exerciseState.value = _exerciseState.value.copy(currentProgress = progress)
    }

    fun clearError() {
        _exerciseState.value = _exerciseState.value.copy(error = null)
    }
}