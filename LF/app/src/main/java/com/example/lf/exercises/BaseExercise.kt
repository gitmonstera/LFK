package com.example.lf.exercises

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max

abstract class BaseExercise {
    var name: String = "Базовое упражнение"
    var description: String = ""
    var exerciseId: String = "base"

    // Состояние
    var state: String = "waiting"
    var holdStart: Long = 0L
    var holdDuration: Long = 700L

    // Прогресс
    var currentCycle: Int = 0
    var currentFinger: Int = 0
    var totalCycles: Int = 5

    // Флаги
    var completed: Boolean = false
    var autoReset: Boolean = false
    var cycleCompleted: Boolean = false
    var calibrated: Boolean = false

    // Калибровка
    var calibrationStart: Long = 0L
    var calibrationDuration: Long = 1500L
    var fingerSizes: FloatArray = FloatArray(5) { 0f }

    // Пороги
    var baseThreshold: Float = 0.045f
    var touchThreshold: Float = 0.045f
    var touchCooldown: Long = 200L
    var lastTouchTime: Long = 0L

    companion object {
        val FINGER_TIPS = intArrayOf(4, 8, 12, 16, 20)
        val FINGER_MCP = intArrayOf(1, 5, 9, 13, 17)
        val FINGER_NAMES = arrayOf("большим", "указательным", "средним", "безымянным", "мизинцем")

        val COLORS = mapOf(
            "yellow" to Color.rgb(0, 255, 255),
            "green" to Color.rgb(0, 255, 0),
            "cyan" to Color.rgb(255, 255, 0),
            "gray" to Color.rgb(128, 128, 128),
            "red" to Color.rgb(255, 0, 0),
            "white" to Color.rgb(255, 255, 255),
            "black" to Color.rgb(0, 0, 0)
        )
    }

    open fun reset() {
        state = "waiting"
        currentCycle = 0
        currentFinger = 0
        completed = false
        autoReset = false
        cycleCompleted = false
        calibrated = false
        holdStart = 0L
        lastTouchTime = 0L
        fingerSizes = FloatArray(5) { 0f }
        calibrationStart = 0L
    }

    open fun getFingerStates(landmarks: List<NormalizedLandmark>, frameWidth: Int, frameHeight: Int): Pair<List<Boolean>, List<Pair<Int, Int>>> {
        val tipPositions = mutableListOf<Pair<Int, Int>>()
        val fingerStates = MutableList(5) { false }

        for (i in 0 until 5) {
            val tip = landmarks[FINGER_TIPS[i]]
            val x = (tip.x() * frameWidth).toInt()
            val y = (tip.y() * frameHeight).toInt()
            tipPositions.add(Pair(x, y))

            if (i == 0) {
                val indexMcp = landmarks[5]
                val dist = abs(tip.x() - indexMcp.x()) + abs(tip.y() - indexMcp.y())
                fingerStates[i] = dist > 0.15f
            } else {
                val pip = landmarks[FINGER_TIPS[i] - 1]
                fingerStates[i] = tip.y() < pip.y() - 0.02f
            }
        }

        return Pair(fingerStates, tipPositions)
    }

    open fun getProgressPercent(): Int {
        val totalTouches = currentCycle * 4 + currentFinger
        val totalNeeded = totalCycles * 4
        return if (totalNeeded > 0) (totalTouches.toFloat() / totalNeeded * 100).toInt() else 0
    }

    open fun getStateMessage(): String {
        if (completed) return "🎉 Упражнение завершено! $totalCycles циклов"

        val nextCycle = min(currentCycle + 1, totalCycles)

        if (!calibrated) {
            val remaining = (calibrationDuration - (System.currentTimeMillis() - calibrationStart)) / 1000
            return "🔧 Калибровка... держите пальцы раскрытыми (${remaining.toInt() + 1}с)"
        }

        val fingerName = when (currentFinger) {
            0 -> "указательным"
            1 -> "средним"
            2 -> "безымянным"
            3 -> "мизинцем"
            else -> ""
        }

        return if (state == "waiting") {
            if (currentFinger == 0) "Коснитесь указательным пальцем (цикл $nextCycle/$totalCycles)"
            else "Коснитесь $fingerName пальцем (цикл $nextCycle/$totalCycles)"
        } else {
            val remaining = (holdDuration - (System.currentTimeMillis() - holdStart)) / 1000
            if (currentFinger == 0) "Держите... ${remaining.toInt() + 1}с (цикл $nextCycle/$totalCycles)"
            else "Держите... ${remaining.toInt() + 1}с"
        }
    }

    open fun getStructuredData(): Map<String, Any> {
        val data = mutableMapOf<String, Any>()
        data["state"] = state
        data["current_cycle"] = currentCycle
        data["total_cycles"] = totalCycles
        data["current_finger"] = currentFinger
        data["progress_percent"] = getProgressPercent()
        data["message"] = getStateMessage()
        data["cycle_completed"] = cycleCompleted
        data["completed"] = completed
        data["auto_reset"] = autoReset
        data["calibrated"] = calibrated

        if (state == "holding" && holdStart > 0) {
            val remaining = ((holdDuration - (System.currentTimeMillis() - holdStart)) / 1000).toInt() + 1
            data["countdown"] = remaining
        }

        return data
    }

    abstract fun getFingerColors(fingerStates: List<Boolean>): List<Int>
    abstract fun checkFingers(
        fingerStates: List<Boolean>,
        landmarks: List<NormalizedLandmark>,
        frameWidth: Int,
        frameHeight: Int
    ): Pair<Boolean, String>

    open fun drawFeedback(
        canvas: Canvas,
        fingerStates: List<Boolean>,
        tipPositions: List<Pair<Int, Int>>,
        isCorrect: Boolean,
        message: String,
        frameWidth: Int,
        frameHeight: Int
    ) {
        val colors = getFingerColors(fingerStates)
        val paint = Paint().apply { style = Paint.Style.FILL }
        val strokePaint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = COLORS["white"]!!
        }
        val textPaint = Paint().apply {
            color = COLORS["white"]!!
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

        // Рисуем кружки на пальцах
        for (i in tipPositions.indices) {
            val (x, y) = tipPositions[i]
            val color = if (i < colors.size) colors[i] else COLORS["gray"]!!
            paint.color = color

            val radius = if (calibrated) {
                min(20 + (fingerSizes[i] * 50).toInt(), 30)
            } else 22

            canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), paint)
            canvas.drawCircle(x.toFloat(), y.toFloat(), radius.toFloat(), strokePaint)
            canvas.drawText((i + 1).toString(), x.toFloat(), y.toFloat() + 8, textPaint)
        }

        // Информационная панель
        val bgPaint = Paint().apply {
            color = COLORS["black"]!!
            alpha = 200
        }
        canvas.drawRect(5f, 5f, 400f, 95f, bgPaint)

        val titlePaint = Paint().apply {
            color = COLORS["white"]!!
            textSize = 20f
        }
        canvas.drawText(name.take(12), 15f, 28f, titlePaint)

        if (!calibrated) {
            val calibPaint = Paint().apply {
                color = COLORS["yellow"]!!
                textSize = 16f
            }
            canvas.drawText("КАЛИБРОВКА...", 15f, 48f, calibPaint)
        } else {
            val progress = getProgressPercent()
            val barWidth = (progress / 100f * 250f).toInt()
            val barPaint = Paint().apply { color = COLORS["green"]!! }
            canvas.drawRect(15f, 40f, (15 + barWidth).toFloat(), 52f, barPaint)

            val progressPaint = Paint().apply {
                color = COLORS["white"]!!
                textSize = 16f
            }
            canvas.drawText("$progress%", 280f, 50f, progressPaint)
        }

        val msgPaint = Paint().apply {
            color = if (isCorrect) COLORS["green"]!! else COLORS["red"]!!
            textSize = 16f
        }
        canvas.drawText(message.take(35), 15f, 75f, msgPaint)
    }
}