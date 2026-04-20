package com.example.lf.exercises

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class FistExercise : BaseExercise() {

    init {
        name = "Кулак"
        description = "Сжимайте и разжимайте кулаки для укрепления кистей"
        exerciseId = "fist"
        totalCycles = 10
        holdDuration = 1500L
    }

    override fun reset() {
        super.reset()
        state = "waiting_fist"
        currentCycle = 0
        completed = false
        calibrated = true  // Для этого упражнения калибровка не нужна
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
            return Pair(true, "🎉 Упражнение завершено!")
        }

        val raisedFingers = fingerStates.count { it }
        val currentTime = System.currentTimeMillis()

        // Кулак: 0-1 палец поднят (большой может быть)
        val isFist = raisedFingers <= 1

        when (state) {
            "waiting_fist" -> {
                if (isFist) {
                    state = "holding_fist"
                    holdStart = currentTime
                }
            }
            "holding_fist" -> {
                if (!isFist) {
                    state = "waiting_fist"
                } else if (currentTime - holdStart >= holdDuration) {
                    currentCycle++
                    state = "waiting_fist"
                    cycleCompleted = true

                    if (currentCycle >= totalCycles) {
                        completed = true
                        autoReset = true
                    }
                }
            }
        }

        val message = when {
            completed -> "🎉 Упражнение завершено!"
            state == "holding_fist" -> {
                val remaining = (holdDuration - (currentTime - holdStart)) / 1000
                "Держите кулак... ${remaining.toInt() + 1}с (${currentCycle + 1}/$totalCycles)"
            }
            else -> "Сожмите кулак (${currentCycle + 1}/$totalCycles)"
        }

        return Pair(state == "holding_fist", message)
    }

    override fun getFingerColors(fingerStates: List<Boolean>): List<Int> {
        val colors = mutableListOf<Int>()
        val isHolding = state == "holding_fist"

        for (isRaised in fingerStates) {
            colors.add(
                if (isHolding && !isRaised) COLORS["green"]!!
                else if (!isHolding && !isRaised) COLORS["cyan"]!!
                else COLORS["red"]!!
            )
        }
        return colors
    }
}