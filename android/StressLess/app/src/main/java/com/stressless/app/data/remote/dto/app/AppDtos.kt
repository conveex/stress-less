package com.stressless.app.data.remote.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class AppHomeResponseDto(
    val user: AppUserSummaryDto,
    val stress: AppStressSummaryDto,
    val band: AppBandSummaryDto? = null,
    val hub: AppHubSummaryDto? = null,
    val room: AppRoomSummaryDto? = null,
    val activeProfile: AppProfileSummaryDto? = null,
    val lastCommand: AppCommandSummaryDto? = null
)

@Serializable
data class AppUserSummaryDto(
    val userId: String,
    val name: String,
    val email: String? = null,
    val isCalibrated: Boolean
)

@Serializable
data class AppStressSummaryDto(
    val detectedState: String,
    val confidence: Double,
    val bpmCurrent: Double? = null,
    val gsrCurrent: Double? = null,
    val bpmBaseline: Double? = null,
    val gsrBaseline: Double? = null,
    val movementAtDetection: Double? = null,
    val reason: String? = null,
    val detectedAt: String? = null
)

@Serializable
data class AppBandSummaryDto(
    val bandId: String,
    val bandLogicalId: String,
    val status: String,
    val isActive: Boolean,
    val batteryLevel: Int? = null,
    val lastSeenAt: String? = null
)

@Serializable
data class AppHubSummaryDto(
    val hubId: String,
    val hubLogicalId: String,
    val status: String,
    val operationalState: String,
    val firmwareVersion: String? = null,
    val lastSeenAt: String? = null,
    val ipAddress: String? = null
)

@Serializable
data class AppRoomSummaryDto(
    val roomId: String,
    val name: String
)

@Serializable
data class AppProfileSummaryDto(
    val profileId: String,
    val name: String,
    val targetState: String
)

@Serializable
data class AppCommandSummaryDto(
    val commandId: String? = null,
    val source: String,
    val status: String,
    val sentAt: String? = null,
    val acknowledgedAt: String? = null
)