package com.stressless.app.data.remote.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class RoomPrimaryResponseDto(
    val roomId: String,
    val name: String,
    val hub: RoomHubResponseDto? = null,
    val devices: List<RoomDeviceResponseDto> = emptyList()
)

@Serializable
data class RoomHubResponseDto(
    val hubId: String,
    val hubLogicalId: String,
    val status: String,
    val operationalState: String,
    val firmwareVersion: String? = null,
    val lastSeenAt: String? = null,
    val ipAddress: String? = null
)

@Serializable
data class RoomDeviceResponseDto(
    val deviceId: String,
    val deviceKey: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val capabilities: List<String> = emptyList(),
    val currentState: String? = null
)