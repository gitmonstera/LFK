package com.example.lf.exercises

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

class FistPalmExercise : BaseExercise() {

    companion object {

        private const val DEFAULT_TOTAL_CYCLES = 5
        private const val DEFAULT_HOLD_DURATION_MS = 2500L

        private const val STATE_WAITING_FIST = "waiting_fist"
        private const val STATE_HOLDING_FIST = "holding_fist"
        private const val STATE_WAITING_PALM = "waiting_palm"
        private const val STATE_HOLDING_PALM = "holding_palm"
        private const val STATE_COMPLETED = "completed"

        private const val MAX_FINGERS_FOR_FIST = 1
        private const val MIN_FINGERS_FOR_PALM = 4

        private const val MSG_NEED_CALIBRATION = "✋ Покажите раскрытую ладонь для калибровки"
        private const val MSG_COMPLETED = "🎉 Упражнение завершено! Отличная работа!"
    }

    private var calibrationData: List<NormalizedLandmark>? = null
    override var calibrated: Boolean = false

    init {
        name = "Кулак-ладонь"
        description = "Чередование кулака и ладони для улучшения кровообращения"
        exerciseId = "fist-palm"
        totalCycles = DEFAULT_TOTAL_CYCLES
        holdDuration = DEFAULT_HOLD_DURATION_MS
    }

    override fun reset() {
        super.reset()
        state = STATE_WAITING_FIST
        currentCycle = 0
        completed = false
        calibrated = false
        calibrationData = null
        autoReset = false
    }

    fun calibrate(landmarks: List<NormalizedLandmark>) {
        calibrationData = landmarks.toList()
        calibrated = true
        state = STATE_WAITING_FIST
        currentCycle = 0
        completed = false
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
            return Pair(true, MSG_COMPLETED)
        }

        if (!calibrated) {
            return Pair(false, MSG_NEED_CALIBRATION)
        }

        val raisedFingers = fingerStates.count { it }
        val isFist = raisedFingers <= MAX_FINGERS_FOR_FIST
        val isPalm = raisedFingers >= MIN_FINGERS_FOR_PALM
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
                    state = STATE_WAITING_PALM
                }
            }
            STATE_WAITING_PALM -> {
                if (isPalm) {
                    state = STATE_HOLDING_PALM
                    holdStart = currentTime
                }
            }
            STATE_HOLDING_PALM -> {
                if (!isPalm) {
                    state = STATE_WAITING_PALM
                } else if (currentTime - holdStart >= holdDuration) {
                    currentCycle++
                    cycleCompleted = true

                    if (currentCycle >= totalCycles) {
                        completed = true
                        autoReset = true
                    } else {
                        state = STATE_WAITING_FIST
                    }
                }
            }
        }

        val message = buildMessage(raisedFingers)
        val isHolding = (state == STATE_HOLDING_FIST || state == STATE_HOLDING_PALM)

        return Pair(isHolding, message)
    }

    private fun buildMessage(raisedFingers: Int): String {
        val cycleInfo = "${currentCycle + 1}/$totalCycles"
        val remainingSec = if (state == STATE_HOLDING_FIST || state == STATE_HOLDING_PALM) {
            val elapsed = System.currentTimeMillis() - holdStart
            val remaining = (holdDuration - elapsed) / 1000
            (remaining + 1).toInt()
        } else null

        return when (state) {
            STATE_COMPLETED -> MSG_COMPLETED
            STATE_HOLDING_FIST -> "Держите кулак... ${remainingSec}с ($cycleInfo)"
            STATE_HOLDING_PALM -> "Держите ладонь... ${remainingSec}с ($cycleInfo)"
            STATE_WAITING_FIST -> {
                val hint = if (raisedFingers > MAX_FINGERS_FOR_FIST)
                    " (поднято пальцев: $raisedFingers, нужно ≤ $MAX_FINGERS_FOR_FIST)"
                else ""
                "Сожмите кулак$hint ($cycleInfo)"
            }
            STATE_WAITING_PALM -> {
                val hint = if (raisedFingers < MIN_FINGERS_FOR_PALM)
                    " (поднято пальцев: $raisedFingers, нужно ≥ $MIN_FINGERS_FOR_PALM)"
                else ""
                "Раскройте ладонь$hint ($cycleInfo)"
            }
            else -> "Ожидание ($cycleInfo)"
        }
    }

    override fun getFingerColors(fingerStates: List<Boolean>): List<Int> {
        val isHolding = (state == STATE_HOLDING_FIST || state == STATE_HOLDING_PALM)
        val isFistPhase = (state == STATE_WAITING_FIST || state == STATE_HOLDING_FIST)
        val isPalmPhase = (state == STATE_WAITING_PALM || state == STATE_HOLDING_PALM)

        return fingerStates.map { isRaised ->
            when {

                isFistPhase && !isRaised && isHolding -> COLORS["green"]!!
                isFistPhase && !isRaised -> COLORS["cyan"]!!

                isPalmPhase && isRaised && isHolding -> COLORS["green"]!!
                isPalmPhase && isRaised -> COLORS["cyan"]!!

                else -> COLORS["red"]!!
            }
        }
    }

    fun forceResetIfNeeded(): Boolean {
        return if (completed || state == STATE_COMPLETED) {
            reset()
            true
        } else false
    }
}