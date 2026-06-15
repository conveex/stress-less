package com.stressless.app.data.remote.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class StressRecentEventsResponseDto(
    val events: List<StressRecentEventResponseDto> = emptyList()
)

@Serializable
data class StressRecentEventResponseDto(
    val stateId: String,
    val state: String,
    val confidence: Double,
    val profileApplied: String? = null,
    val detectedAt: String? = null,
    val resolvedAt: String? = null,
    val durationMinutes: Long? = null
)