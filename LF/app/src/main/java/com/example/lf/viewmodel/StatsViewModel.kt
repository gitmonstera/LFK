package com.example.lf.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lf.api.ApiClient
import com.example.lf.api.DailyStatsResponse
import com.example.lf.api.ExerciseResponse
import com.example.lf.api.ExerciseResultRequest
import com.example.lf.api.ExerciseStatResponse
import com.example.lf.api.OverallStatsResponse
import com.example.lf.api.WorkoutHistoryResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsViewModel : ViewModel() {

    // Overall stats
    private val _overallStats = MutableLiveData<OverallStatsResponse?>()
    val overallStats: LiveData<OverallStatsResponse?> = _overallStats

    // Daily stats
    private val _dailyStats = MutableLiveData<DailyStatsResponse?>()
    val dailyStats: LiveData<DailyStatsResponse?> = _dailyStats

    private val _selectedDate = MutableLiveData(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: LiveData<String> = _selectedDate

    // Weekly stats
    private val _weeklyStats = MutableLiveData<List<DailyStatsResponse>>()
    val weeklyStats: LiveData<List<DailyStatsResponse>> = _weeklyStats

    // Monthly stats
    private val _monthlyStats = MutableLiveData<List<DailyStatsResponse>>()
    val monthlyStats: LiveData<List<DailyStatsResponse>> = _monthlyStats

    // Exercise stats
    private val _exerciseStats = MutableLiveData<List<ExerciseStatResponse>>()
    val exerciseStats: LiveData<List<ExerciseStatResponse>> = _exerciseStats

    // Workout history
    private val _workoutHistory = MutableLiveData<List<WorkoutHistoryResponse>>()
    val workoutHistory: LiveData<List<WorkoutHistoryResponse>> = _workoutHistory

    // Exercises list
    private val _exercises = MutableLiveData<List<ExerciseResponse>>()
    val exercises: LiveData<List<ExerciseResponse>> = _exercises

    // Loading states
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Current tab
    private val _currentTab = MutableLiveData(0)
    val currentTab: LiveData<Int> = _currentTab

    fun setCurrentTab(index: Int) {
        _currentTab.value = index
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun loadExercises(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = ApiClient.apiService.getExerciseList("Bearer $token")
                if (response.isSuccessful) {
                    _exercises.value = response.body()?.items ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadOverallStats(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getOverallStats("Bearer $token")
                if (response.isSuccessful) {
                    _overallStats.value = response.body()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadWeeklyStats(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getWeeklyStats("Bearer $token")
                if (response.isSuccessful) {
                    _weeklyStats.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMonthlyStats(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getMonthlyStats("Bearer $token")
                if (response.isSuccessful) {
                    _monthlyStats.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadExerciseStats(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getExerciseStats("Bearer $token")
                if (response.isSuccessful) {
                    _exerciseStats.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadWorkoutHistory(token: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.getWorkoutHistory("Bearer $token")
                if (response.isSuccessful) {
                    _workoutHistory.value = response.body() ?: emptyList()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadAllStats(token: String) {
        loadOverallStats(token)
        loadWeeklyStats(token)
        loadMonthlyStats(token)
        loadExerciseStats(token)
        loadWorkoutHistory(token)
        loadExercises(token)
    }

    fun clearError() {
        _error.value = null
    }

    // Добавьте в StatsViewModel.kt
    fun saveExerciseResult(token: String, exerciseId: String, repetitions: Int, duration: Int, accuracy: Float) {
        viewModelScope.launch {
            try {
                // Сначала получаем или создаем сессию
                val sessionResponse = ApiClient.apiService.startWorkout("Bearer $token")
                if (sessionResponse.isSuccessful && sessionResponse.body() != null) {
                    val sessionId = sessionResponse.body()!!.id

                    val request = ExerciseResultRequest(
                        session_id = sessionId,
                        exercise_id = exerciseId,
                        actual_repetitions = repetitions,
                        actual_duration = duration,
                        accuracy_score = accuracy
                    )

                    val response = ApiClient.apiService.addExerciseSet("Bearer $token", request)
                    if (response.isSuccessful) {
                        // Завершаем сессию
                        ApiClient.apiService.endWorkout("Bearer $token", mapOf("session_id" to sessionId))
                        android.util.Log.d("StatsViewModel", "Результат сохранен: $exerciseId")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StatsViewModel", "Ошибка сохранения результата", e)
            }
        }
    }
}