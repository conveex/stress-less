package com.stressless.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class StressRecentEventsResponse(
    val events: List<StressRecentEventResponse>
)

@Serializable
data class StressRecentEventResponse(
    val stateId: String,
    val state: String,
    val confidence: Double,
    val profileApplied: String?,
    val detectedAt: String,
    val resolvedAt: String?,
    val durationMinutes: Long?
)