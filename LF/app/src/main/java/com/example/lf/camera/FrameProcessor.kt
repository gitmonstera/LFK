package com.example.lf.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.lf.exercises.BaseExercise
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ExerciseFeedback(
    val isCorrect: Boolean,
    val message: String,
    val progressPercent: Int,
    val fingerStates: List<Boolean>,
    val raisedFingers: Int,
    val structuredData: Map<String, Any> = emptyMap(),
    val processedBitmap: Bitmap? = null
)

class FrameProcessor(private val exercise: BaseExercise) {

    private val _feedback = MutableStateFlow<ExerciseFeedback?>(null)
    val feedback: StateFlow<ExerciseFeedback?> = _feedback.asStateFlow()

    private var frameCounter = 0

    fun processFrame(result: HandLandmarkerResult, frameWidth: Int, frameHeight: Int) {
        frameCounter++

        // Пропускаем каждый 2-й кадр для оптимизации
        if (frameCounter % 2 != 0 && frameCounter > 1) {
            return
        }

        val landmarks = result.landmarks()
        if (landmarks.isEmpty()) {
            _feedback.value = ExerciseFeedback(
                isCorrect = false,
                message = "Рука не обнаружена",
                progressPercent = 0,
                fingerStates = emptyList(),
                raisedFingers = 0,
                processedBitmap = null
            )
            return
        }

        val handLandmarks = landmarks[0]
        val (fingerStates, tipPositions) = exercise.getFingerStates(handLandmarks, frameWidth, frameHeight)
        val raisedFingers = fingerStates.count { it }

        val (isCorrect, message) = exercise.checkFingers(fingerStates, handLandmarks, frameWidth, frameHeight)

        // Создаем Bitmap с отрисовкой
        val bitmap = Bitmap.createBitmap(frameWidth, frameHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Белый фон
        canvas.drawColor(Color.WHITE)

        // Рисуем обратную связь
        exercise.drawFeedback(
            canvas, fingerStates, tipPositions, isCorrect, message, frameWidth, frameHeight
        )

        _feedback.value = ExerciseFeedback(
            isCorrect = isCorrect,
            message = message,
            progressPercent = exercise.getProgressPercent(),
            fingerStates = fingerStates,
            raisedFingers = raisedFingers,
            structuredData = exercise.getStructuredData(),
            processedBitmap = bitmap
        )
    }

    fun reset() {
        exercise.reset()
        frameCounter = 0
    }
}