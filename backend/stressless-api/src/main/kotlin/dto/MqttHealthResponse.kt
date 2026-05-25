package com.stressless.dto

import kotlinx.serialization.Serializable

@Serializable
data class MqttHealthResponse(
    val status: String,
    val connected: Boolean,
    val host: String,
    val port: Int,
    val clientId: String,
    val subscriptions: List<String>,
    val messagesReceived: Long,
    val lastMessageTopic: String?,
    val lastMessagePayloadPreview: String?
)