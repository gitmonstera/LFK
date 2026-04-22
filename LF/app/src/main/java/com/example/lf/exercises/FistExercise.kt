package com.example.lf.exercises

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class FistExercise : BaseExercise() {

    companion object {

        private const val DEFAULT_TOTAL_CYCLES = 10
        private const val DEFAULT_HOLD_DURATION_MS = 1500L

        private const val STATE_WAITING_FIST = "waiting_fist"
        private const val STATE_HOLDING_FIST = "holding_fist"

        private const val MAX_RAISED_FINGERS_FOR_FIST = 1
    }

    init {
        name = "Кулак"
        description = "Сжимайте и разжимайте кулаки для укрепления кистей"
        exerciseId = "fist"
        totalCycles = DEFAULT_TOTAL_CYCLES
        holdDuration = DEFAULT_HOLD_DURATION_MS
    }

    override fun reset() {
        super.reset()
        state = STATE_WAITING_FIST
        currentCycle = 0
        completed = false
        calibrated = true

        autoReset = false
    }

    override fun checkFingers(
        fingerStates: List<Boolean>,
        landmarks: List<NormalizedLandmark>,
        frameWidth: Int,
        frameHeight: Int
    ): Pair<Boolean, String> {

        if (autoReset && completed) {
            reset()
            autoReset = false
        }

        if (completed) {
            return Pair(true, "🎉 Упражнение завершено! Отличная работа!")
        }

        val raisedFingersCount = fingerStates.count { it }
        val isFist = raisedFingersCount <= MAX_RAISED_FINGERS_FOR_FIST
        val currentTime = System.currentTimeMillis()

        when (state) {
            STATE_WAITING_FIST -> {
                if (isFist) {
                    state = STATE_HOLDING_FIST
                    holdStart = currentTime
                }
            }
            STATE_HOLDING_FIST -> {
                if (!isFist) {

                    state = STATE_WAITING_FIST
                } else if (currentTime - holdStart >= holdDuration) {

                    currentCycle++
                    cycleCompleted = true
                    state = STATE_WAITING_FIST

                    if (currentCycle >= totalCycles) {
                        completed = true
                        autoReset = true
                    }
                }
            }
        }

        val message = buildMessage(raisedFingersCount, currentTime)
        val isHolding = (state == STATE_HOLDING_FIST)

        return Pair(isHolding, message)
    }

    private fun buildMessage(raisedFingersCount: Int, currentTime: Long): String {
        return when {
            completed -> "🎉 Упражнение завершено! Отличная работа!"
            state == STATE_HOLDING_FIST -> {
                val elapsed = currentTime - holdStart
                val remainingSeconds = ((holdDuration - elapsed) / 1000).toInt() + 1
                "Держите кулак... ${remainingSeconds}с (${currentCycle + 1}/$totalCycles)"
            }
            else -> {
                val hint = if (raisedFingersCount > MAX_RAISED_FINGERS_FOR_FIST) {
                    " (поднято пальцев: $raisedFingersCount, нужно ≤ $MAX_RAISED_FINGERS_FOR_FIST)"
                } else ""
                "Сожмите кулак$hint (${currentCycle + 1}/$totalCycles)"
            }
        }
    }

    override fun getFingerColors(fingerStates: List<Boolean>): List<Int> {
        val isHolding = state == STATE_HOLDING_FIST
        val colors = mutableListOf<Int>()

        for (isRaised in fingerStates) {
            val color = when {

                isHolding && !isRaised -> COLORS["green"]!!

                !isHolding && !isRaised -> COLORS["cyan"]!!

                else -> COLORS["red"]!!
            }
            colors.add(color)
        }
        return colors
    }
}