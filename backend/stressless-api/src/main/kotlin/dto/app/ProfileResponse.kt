package com.stressless.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesResponse(
    val profiles: List<ProfileResponse>
)

@Serializable
data class ProfileResponse(
    val profileId: String,
    val name: String,
    val targetState: String,
    val isActive: Boolean,
    val useAutomaticFallback: Boolean,
    val actionsCount: Int,
    val createdAt: String
)