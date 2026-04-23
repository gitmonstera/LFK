package com.example.lf.camera

data class HandDetectionResult(
    val landmarks: List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>> = emptyList(),
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val isFrontCamera: Boolean = true
)