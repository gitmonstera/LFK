package com.example.lfk.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lfk.api.ApiClient
import com.example.lfk.models.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class StatsViewModel : ViewModel() {
    private val apiService = ApiClient.instance

    // Overall stats
    private val _overallStats = MutableLiveData<OverallStats?>()
    val overallStats: LiveData<OverallStats?> = _overallStats

    // Daily stats
    private val _dailyStats = MutableLiveData<DailyStats?>()
    val dailyStats: LiveData<DailyStats?> = _dailyStats
    private val _selectedDate = MutableLiveData(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
    val selectedDate: LiveData<String> = _selectedDate

    // Weekly stats
    private val _weeklyStats = MutableLiveData<List<DailyStatItem>>()
    val weeklyStats: LiveData<List<DailyStatItem>> = _weeklyStats

    // Monthly stats (grouped by week)
    private val _monthlyStats = MutableLiveData<List<WeekStats>>()
    val monthlyStats: LiveData<List<WeekStats>> = _monthlyStats

    // Exercise stats
    private val _exerciseStats = MutableLiveData<List<ExerciseStatItem>>()
    val exerciseStats: LiveData<List<ExerciseStatItem>> = _exerciseStats

    // Workout history
    private val _workoutHistory = MutableLiveData<List<WorkoutHistoryItem>>()
    val workoutHistory: LiveData<List<WorkoutHistoryItem>> = _workoutHistory

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

    fun loadOverallStats(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getOverallStats("Bearer $token")
                _overallStats.value = response
                Log.d("StatsViewModel", "Overall stats loaded: $response")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки общей статистики: ${e.message}"
                Log.e("StatsViewModel", "Error loading overall stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDailyStats(token: String, date: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getDailyStats("Bearer $token", date)
                _dailyStats.value = response
                Log.d("StatsViewModel", "Daily stats loaded for $date: $response")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки дневной статистики: ${e.message}"
                Log.e("StatsViewModel", "Error loading daily stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWeeklyStats(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getWeeklyStats("Bearer $token")
                _weeklyStats.value = response
                Log.d("StatsViewModel", "Weekly stats loaded: ${response.size} days")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки недельной статистики: ${e.message}"
                Log.e("StatsViewModel", "Error loading weekly stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMonthlyStats(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getMonthlyStats("Bearer $token")

                // Группируем по неделям
                val weeksMap = mutableMapOf<Int, WeekStats>()

                for (day in response) {
                    val dateStr = day["stat_date"]?.toString() ?: continue
                    val weekNum = try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)?.let {
                            val calendar = Calendar.getInstance()
                            calendar.time = it
                            calendar.get(Calendar.WEEK_OF_YEAR)
                        } ?: 0
                    } catch (e: Exception) {
                        0
                    }

                    val sessions = (day["total_sessions"] as? Double)?.toInt() ?: 0
                    val exercises = (day["total_exercises"] as? Double)?.toInt() ?: 0
                    val duration = (day["total_duration_seconds"] as? Double)?.toInt() ?: 0

                    val existing = weeksMap[weekNum]
                    if (existing != null) {
                        weeksMap[weekNum] = WeekStats(
                            week_number = weekNum,
                            sessions = existing.sessions + sessions,
                            exercises = existing.exercises + exercises,
                            duration = existing.duration + duration
                        )
                    } else {
                        weeksMap[weekNum] = WeekStats(
                            week_number = weekNum,
                            sessions = sessions,
                            exercises = exercises,
                            duration = duration
                        )
                    }
                }

                _monthlyStats.value = weeksMap.values.sortedBy { it.week_number }
                Log.d("StatsViewModel", "Monthly stats loaded: ${weeksMap.size} weeks")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки месячной статистики: ${e.message}"
                Log.e("StatsViewModel", "Error loading monthly stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadExerciseStats(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getExerciseStats("Bearer $token")
                _exerciseStats.value = response
                Log.d("StatsViewModel", "Exercise stats loaded: ${response.size} exercises")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки статистики по упражнениям: ${e.message}"
                Log.e("StatsViewModel", "Error loading exercise stats", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadWorkoutHistory(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = apiService.getWorkoutHistory("Bearer $token")
                _workoutHistory.value = response
                Log.d("StatsViewModel", "Workout history loaded: ${response.size} workouts")
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки истории тренировок: ${e.message}"
                Log.e("StatsViewModel", "Error loading workout history", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadAllStats(token: String) {
        loadOverallStats(token)
        loadDailyStats(token, _selectedDate.value!!)
        loadWeeklyStats(token)
        loadMonthlyStats(token)
        loadExerciseStats(token)
        loadWorkoutHistory(token)
    }

    fun clearError() {
        _error.value = null
    }
}