package com.stressless.dto.mqtt

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class HubEventPayload(
    val hubId: String,
    val eventType: String,
    val severity: String,
    val commandId: String? = null,
    val description: String? = null,
    val metadata: JsonElement? = null,
    val timestamp: String? = null
)