package com.stressless.dto.mqtt

import kotlinx.serialization.Serializable

@Serializable
data class BiometricsPayload(
    val bandId: String,
    val hubId: String,
    val bpm: Double,
    val gsr: Double,
    val movement: Double,
    val battery: Int? = null,
    val source: String,
    val timestamp: String
)