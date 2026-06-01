package com.stressless.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class AppHomeResponse(
    val user: AppUserSummary,
    val stress: AppStressSummary,
    val band: AppBandSummary?,
    val hub: AppHubSummary?,
    val room: AppRoomSummary?,
    val activeProfile: AppProfileSummary?,
    val lastCommand: AppCommandSummary?
)

@Serializable
data class AppUserSummary(
    val userId: String,
    val name: String,
    val email: String,
    val isCalibrated: Boolean
)

@Serializable
data class AppStressSummary(
    val detectedState: String,
    val confidence: Double,
    val bpmCurrent: Double?,
    val gsrCurrent: Double?,
    val bpmBaseline: Double,
    val gsrBaseline: Double,
    val movementAtDetection: Double?,
    val reason: String?,
    val detectedAt: String?
)

@Serializable
data class AppBandSummary(
    val bandId: String,
    val bandLogicalId: String,
    val status: String,
    val isActive: Boolean,
    val batteryLevel: Int?,
    val lastSeenAt: String?
)

@Serializable
data class AppHubSummary(
    val hubId: String,
    val hubLogicalId: String,
    val status: String,
    val operationalState: String,
    val firmwareVersion: String?,
    val lastSeenAt: String?,
    val ipAddress: String?
)

@Serializable
data class AppRoomSummary(
    val roomId: String,
    val name: String
)

@Serializable
data class AppProfileSummary(
    val profileId: String,
    val name: String,
    val targetState: String
)

@Serializable
data class AppCommandSummary(
    val commandId: String?,
    val source: String,
    val status: String,
    val sentAt: String,
    val acknowledgedAt: String?
)