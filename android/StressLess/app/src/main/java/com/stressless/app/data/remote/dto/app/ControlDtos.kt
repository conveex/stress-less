package com.stressless.app.data.remote.dto.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChangeOperationalStateRequestDto(
    val state: String
)

@Serializable
data class ChangeOperationalStateResponseDto(
    val hubId: String,
    val previousState: String,
    val newState: String,
    val changedAt: String
)

@Serializable
data class ManualHubCommandRequestDto(
    val actions: List<ManualHubActionRequestDto>
)

@Serializable
data class ManualHubActionRequestDto(
    val deviceKey: String,
    val action: String,
    val value: JsonElement? = null
)

@Serializable
data class ManualHubCommandResponseDto(
    val commandId: String,
    val status: String,
    val sentAt: String
)