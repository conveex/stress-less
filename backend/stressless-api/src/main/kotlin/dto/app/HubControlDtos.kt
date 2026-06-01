package com.stressless.dto.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChangeOperationalStateRequest(
    val state: String
)

@Serializable
data class ChangeOperationalStateResponse(
    val hubId: String,
    val previousState: String,
    val newState: String,
    val changedAt: String
)

@Serializable
data class ManualHubCommandRequest(
    val actions: List<ManualHubActionRequest>
)

@Serializable
data class ManualHubActionRequest(
    val deviceKey: String,
    val action: String,
    val value: JsonElement? = null
)

@Serializable
data class ManualHubCommandResponse(
    val commandId: String,
    val status: String,
    val sentAt: String
)