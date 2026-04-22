package com.example.lf.exercises

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class FingerTouchingExercise : BaseExercise() {

    init {
        name = "Считалочка"
        description = "Поочередное касание пальцев - развивает мелкую моторику"
        exerciseId = "finger-touching"
        totalCycles = 5
        holdDuration = 700L
    }

    override fun reset() {
        super.reset()
        state = "waiting"
        currentCycle = 0
        currentFinger = 0
        completed = false
        calibrated = false
        fingerSizes = FloatArray(5) { 0f }
    }

    private fun calibrateFingerSizes(landmarks: List<NormalizedLandmark>) {
        if (!calibrated) {
            if (calibrationStart == 0L) {
                calibrationStart = System.currentTimeMillis()
                return
            }

            if (System.currentTimeMillis() - calibrationStart >= calibrationDuration) {
                calibrated = true
                val avgSize = fingerSizes.filter { it > 0 }.average().toFloat()
                touchThreshold = if (avgSize > 0) avgSize * 0.8f else baseThreshold
                android.util.Log.d("FingerTouching", "Калибровка завершена, порог: $touchThreshold")
                return
            }

            for (i in 0 until 5) {
                val tip = landmarks[FINGER_TIPS[i]]
                val mcp = landmarks[FINGER_MCP[i]]
                val size = abs(tip.y() - mcp.y())
                fingerSizes[i] = fingerSizes[i] * 0.7f + size * 0.3f
            }
        }
    }

    private fun getAdaptiveThreshold(fingerIdx: Int): Float {
        if (calibrated && fingerSizes[fingerIdx] > 0) {
            return max(baseThreshold, fingerSizes[fingerIdx] * 0.6f)
        }
        return baseThreshold
    }

    override fun checkFingers(
        fingerStates: List<Boolean>,
        landmarks: List<NormalizedLandmark>,
        frameWidth: Int,
        frameHeight: Int
    ): Pair<Boolean, String> {

        if (autoReset) {
            reset()
            autoReset = false
        }

        if (completed) {
            return Pair(true, getStateMessage())
        }

        calibrateFingerSizes(landmarks)

        if (!calibrated) {
            return Pair(true, getStateMessage())
        }

        val currentTime = System.currentTimeMillis()

        val thumb = landmarks[FINGER_TIPS[0]]
        val targetIdx = currentFinger + 1
        val target = landmarks[FINGER_TIPS[targetIdx]]

        val dx = thumb.x() - target.x()
        val dy = thumb.y() - target.y()
        val distance = abs(dx) + abs(dy)

        val threshold = getAdaptiveThreshold(targetIdx)
        val fingerSize = fingerSizes[targetIdx]
        val normalizedDistance = distance / max(fingerSize, 0.01f)
        val isTouching = distance < threshold || normalizedDistance < 0.5f

        cycleCompleted = false

        when (state) {
            "waiting" -> {
                if (isTouching && currentTime - lastTouchTime > touchCooldown) {
                    state = "holding"
                    holdStart = currentTime
                    lastTouchTime = currentTime
                }
            }
            "holding" -> {
                val releaseThreshold = threshold * 1.3f
                if (distance >= releaseThreshold) {
                    state = "waiting"
                } else if (currentTime - holdStart >= holdDuration) {
                    state = "waiting"
                    currentFinger++
                    cycleCompleted = true

                    if (currentFinger >= 4) {
                        currentFinger = 0
                        currentCycle++

                        if (currentCycle >= totalCycles) {
                            completed = true
                            autoReset = true
                            android.util.Log.d("FingerTouching", "Упражнение завершено!")
                        }
                    }
                }
            }
        }

        return Pair(true, getStateMessage())
    }

    override fun getFingerColors(fingerStates: List<Boolean>): List<Int> {
        val colors = mutableListOf<Int>()
        val target = currentFinger + 1
        val isHolding = state == "holding"

        for (i in 0 until 5) {
            when {
                i == 0 -> colors.add(COLORS["yellow"]!!)
                i < target -> colors.add(COLORS["green"]!!)
                i == target -> colors.add(if (isHolding) COLORS["green"]!! else COLORS["cyan"]!!)
                else -> colors.add(COLORS["gray"]!!)
            }
        }
        return colors
    }

    override fun drawFeedback(
        canvas: Canvas,
        fingerStates: List<Boolean>,
        tipPositions: List<Pair<Int, Int>>,
        isCorrect: Boolean,
        message: String,
        frameWidth: Int,
        frameHeight: Int
    ) {

        val colors = getFingerColors(fingerStates)
        for (i in tipPositions.indices) {
            val (x, y) = tipPositions[i]
            val color = if (i < colors.size) colors[i] else COLORS["gray"]!!

            val baseRadius = 25f
            val radius = if (calibrated && i < fingerSizes.size) {
                min(baseRadius + fingerSizes[i] * 80f, 40f)
            } else {
                baseRadius
            }

            canvas.drawCircle(x.toFloat(), y.toFloat(), radius, Paint().apply {
                this.color = color
                style = Paint.Style.FILL
                isAntiAlias = true
            })

            canvas.drawCircle(x.toFloat(), y.toFloat(), radius, Paint().apply {
                this.color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 3f
                isAntiAlias = true
            })

            canvas.drawText((i + 1).toString(), x.toFloat(), y.toFloat() + 6f, Paint().apply {
                this.color = Color.WHITE
                textSize = 18f
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
                setShadowLayer(4f, 0f, 0f, Color.BLACK)
            })
        }

        canvas.drawRect(5f, 5f, 400f, 95f, Paint().apply {
            color = Color.argb(180, 0, 0, 0)
            style = Paint.Style.FILL
        })

        canvas.drawText(name, 15f, 28f, Paint().apply {
            color = Color.WHITE
            textSize = 18f
            isAntiAlias = true
        })

        if (!calibrated) {
            canvas.drawText("КАЛИБРОВКА...", 15f, 48f, Paint().apply {
                color = Color.CYAN
                textSize = 16f
                isAntiAlias = true
            })
        } else {
            val progress = getProgressPercent()
            val barWidth = (progress / 100f) * 250f

            canvas.drawRect(15f, 40f, 15f + 250f, 52f, Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.FILL
            })

            canvas.drawRect(15f, 40f, 15f + barWidth, 52f, Paint().apply {
                color = Color.GREEN
                style = Paint.Style.FILL
            })

            canvas.drawText("$progress%", 280f, 50f, Paint().apply {
                color = Color.WHITE
                textSize = 14f
                isAntiAlias = true
            })
        }

        canvas.drawText(message.take(35), 15f, 75f, Paint().apply {
            color = Color.LTGRAY
            textSize = 14f
            isAntiAlias = true
        })
    }
}