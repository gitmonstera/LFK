package com.example.lf.exercises

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class FistPalmExercise : BaseExercise() {

    init {
        name = "Кулак-ладонь"
        description = "Чередование кулака и ладони"
        exerciseId = "fist-palm"
        totalCycles = 5
        holdDuration = 2500L
    }

    override fun reset() {
        super.reset()
        state = "waiting_fist"
        currentCycle = 0
        completed = false
        calibrated = true
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
        val isFist = raisedFingers <= 1
        val isPalm = raisedFingers >= 4

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
                    state = "waiting_palm"
                }
            }
            "waiting_palm" -> {
                if (isPalm) {
                    state = "holding_palm"
                    holdStart = currentTime
                }
            }
            "holding_palm" -> {
                if (!isPalm) {
                    state = "waiting_palm"
                } else if (currentTime - holdStart >= holdDuration) {
                    currentCycle++
                    if (currentCycle >= totalCycles) {
                        completed = true
                        autoReset = true
                    } else {
                        state = "waiting_fist"
                    }
                    cycleCompleted = true
                }
            }
        }

        val message = when {
            completed -> "🎉 Упражнение завершено!"
            state == "holding_fist" -> {
                val remaining = (holdDuration - (currentTime - holdStart)) / 1000
                "Держите кулак... ${remaining.toInt() + 1}с (${currentCycle + 1}/$totalCycles)"
            }
            state == "holding_palm" -> {
                val remaining = (holdDuration - (currentTime - holdStart)) / 1000
                "Держите ладонь... ${remaining.toInt() + 1}с (${currentCycle + 1}/$totalCycles)"
            }
            state == "waiting_fist" -> "Сожмите кулак (${currentCycle + 1}/$totalCycles)"
            else -> "Раскройте ладонь (${currentCycle + 1}/$totalCycles)"
        }

        return Pair(state.contains("holding"), message)
    }

    override fun getFingerColors(fingerStates: List<Boolean>): List<Int> {
        val colors = mutableListOf<Int>()
        val isFistPhase = state == "waiting_fist" || state == "holding_fist"
        val isPalmPhase = state == "waiting_palm" || state == "holding_palm"
        val isHolding = state == "holding_fist" || state == "holding_palm"

        for (isRaised in fingerStates) {
            colors.add(
                when {
                    isFistPhase && !isRaised && isHolding -> COLORS["green"]!!
                    isFistPhase && !isRaised -> COLORS["cyan"]!!
                    isPalmPhase && isRaised && isHolding -> COLORS["green"]!!
                    isPalmPhase && isRaised -> COLORS["cyan"]!!
                    else -> COLORS["red"]!!
                }
            )
        }
        return colors
    }
}