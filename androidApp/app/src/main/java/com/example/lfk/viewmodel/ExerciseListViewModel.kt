package com.example.lfk.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lfk.api.ApiClient
import com.example.lfk.models.ExerciseCategory
import com.example.lfk.models.ServerExercise
import kotlinx.coroutines.launch

class ExerciseListViewModel : ViewModel() {
    private val apiService = ApiClient.instance

    private val _exercises = MutableLiveData<List<ServerExercise>>()
    val exercises: LiveData<List<ServerExercise>> = _exercises

    private val _categories = MutableLiveData<List<ExerciseCategory>>()
    val categories: LiveData<List<ExerciseCategory>> = _categories

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadExercises(token: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val response = apiService.getExerciseList("Bearer $token")
                _exercises.value = response.items

                // Группируем упражнения по категориям
                groupExercisesByCategory(response.items)

            } catch (e: Exception) {
                _error.value = "Ошибка загрузки упражнений: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun groupExercisesByCategory(exercises: List<ServerExercise>) {
        val groupedMap = exercises.groupBy { it.category }

        val categoryList = groupedMap.map { (categoryName, exercisesInCategory) ->
            // Берем первую иконку и цвет из упражнений категории
            val firstExercise = exercisesInCategory.firstOrNull()
            ExerciseCategory(
                name = categoryName,
                icon = firstExercise?.category_icon,
                color = firstExercise?.category_color,
                exercises = exercisesInCategory
            )
        }.sortedBy { it.name }

        _categories.value = categoryList
    }

    fun getExercisesByCategory(categoryName: String): List<ServerExercise> {
        return _categories.value?.find { it.name == categoryName }?.exercises ?: emptyList()
    }

    fun getExerciseById(exerciseId: String): ServerExercise? {
        return _exercises.value?.find { it.exercise_id == exerciseId }
    }

    fun clearError() {
        _error.value = null
    }
}