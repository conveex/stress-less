package com.stressless.dto.stress

data class StressClassificationResult(
    val state: String,
    val confidence: Double,
    val bpmDelta: Double,
    val gsrDelta: Double,
    val movementAtDetection: Double,
    val reasonJson: String
)