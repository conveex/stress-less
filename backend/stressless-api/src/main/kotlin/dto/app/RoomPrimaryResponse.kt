package com.stressless.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class RoomPrimaryResponse(
    val roomId: String,
    val name: String,
    val hub: RoomHubResponse?,
    val devices: List<RoomDeviceResponse>
)

@Serializable
data class RoomHubResponse(
    val hubId: String,
    val hubLogicalId: String,
    val status: String,
    val operationalState: String,
    val firmwareVersion: String?,
    val lastSeenAt: String?,
    val ipAddress: String?
)

@Serializable
data class RoomDeviceResponse(
    val deviceId: String,
    val deviceKey: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val capabilities: List<String>,
    val currentState: String?
)