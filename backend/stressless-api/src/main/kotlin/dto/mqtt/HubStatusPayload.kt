package com.stressless.dto.mqtt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HubStatusPayload(
    val hubId: String,
    val status: String,

    @SerialName("operational_state")
    val operationalState: String? = null,

    @SerialName("firmware_version")
    val firmwareVersion: String? = null,

    @SerialName("ip_address")
    val ipAddress: String? = null,

    @SerialName("free_heap")
    val freeHeap: Long? = null,

    @SerialName("uptime_seconds")
    val uptimeSeconds: Long? = null,

    val timestamp: String? = null
)