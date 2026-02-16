package com.example.lfk.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val username: String,
    val email: String
)

data class Exercise(
    val id: String,
    val name: String,
    val description: String
)

data class FrameFeedback(
    @SerializedName("hand_detected")
    val handDetected: Boolean,
    @SerializedName("raised_fingers")
    val raisedFingers: Int,
    @SerializedName("finger_states")
    val fingerStates: List<Boolean>,
    val message: String,
    @SerializedName("processed_frame")
    val processedFrame: String,
    val structured: StructuredData?
)

data class StructuredData(
    val state: String,
    @SerializedName("state_name")
    val stateName: String,
    @SerializedName("current_cycle")
    val currentCycle: Int,
    @SerializedName("total_cycles")
    val totalCycles: Int,
    val countdown: Int?,
    @SerializedName("progress_percent")
    val progressPercent: Double,
    val message: String
)