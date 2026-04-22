package com.example.lf.api

import com.google.gson.annotations.SerializedName

data class ExerciseResponse(
    @SerializedName("exercise_id") val exerciseId: String,
    val name: String,
    val description: String,
    val category: String,
    @SerializedName("category_id") val categoryId: Int,
    @SerializedName("category_icon") val categoryIcon: String?,
    @SerializedName("category_color") val categoryColor: String?,
    @SerializedName("difficulty_level") val difficultyLevel: Int,
    @SerializedName("target_muscles") val targetMuscles: List<String>,
    val instructions: List<String>,
    @SerializedName("duration_seconds") val durationSeconds: Int,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("video_url") val videoUrl: String?,
    @SerializedName("applicable_codes") val applicableCodes: List<String> = emptyList()
)