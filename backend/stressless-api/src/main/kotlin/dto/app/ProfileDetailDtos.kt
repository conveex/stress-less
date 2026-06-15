package com.stressless.dto.app

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ProfileDetailResponse(
    val profileId: String,
    val name: String,
    val targetState: String,
    val isActive: Boolean,
    val useAutomaticFallback: Boolean,
    val actions: List<ProfileActionResponse>
)

@Serializable
data class ProfileActionResponse(
    val actionId: String,
    val deviceId: String,
    val deviceKey: String,
    val deviceName: String,
    val deviceType: String,
    val action: String,
    val value: JsonElement,
    val orderIndex: Int
)

@Serializable
data class CreateProfileRequest(
    val name: String,
    val targetState: String,
    val isActive: Boolean = true,
    val useAutomaticFallback: Boolean = true,
    val actions: List<SaveProfileActionRequest> = emptyList()
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val targetState: String,
    val isActive: Boolean,
    val useAutomaticFallback: Boolean,
    val actions: List<SaveProfileActionRequest> = emptyList()
)

@Serializable
data class SaveProfileActionRequest(
    val deviceId: String,
    val action: String,
    val value: JsonElement,
    val orderIndex: Int
)

@Serializable
data class UpdateProfileActiveRequest(
    val isActive: Boolean
)

@Serializable
data class SaveProfileResponse(
    val profileId: String,
    val message: String
)