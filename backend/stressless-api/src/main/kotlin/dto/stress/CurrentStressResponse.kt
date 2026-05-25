package com.stressless.dto.stress

import kotlinx.serialization.Serializable

@Serializable
data class CurrentStressResponse(
    val userId: String,
    val detectedState: String,
    val confidence: Double,
    val bpmDelta: Double?,
    val gsrDelta: Double?,
    val movementAtDetection: Double?,
    val reason: String?,
    val detectedAt: String?
)