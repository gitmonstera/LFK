package com.example.lfk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель упражнения с сервера
 */
@Parcelize
data class ServerExercise(
    val exercise_id: String,
    val name: String,
    val description: String,
    val category: String,
    val category_icon: String?,
    val category_color: String?,
    val difficulty_level: Int,
    val target_muscles: List<String>,
    val instructions: List<String>,
    val duration_seconds: Int,
    val image_url: String?,
    val video_url: String?
) : Parcelable

/**
 * Ответ сервера со списком упражнений
 */
@Parcelize
data class ExerciseListResponse(
    val items: List<ServerExercise>
) : Parcelable

/**
 * Категория упражнений для отображения
 */
data class ExerciseCategory(
    val name: String,
    val icon: String?,
    val color: String?,
    val exercises: List<ServerExercise>
)