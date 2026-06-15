package com.stressless.app.data.remote.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class ProfilesResponseDto(
    val profiles: List<ProfileResponseDto> = emptyList()
)

@Serializable
data class ProfileResponseDto(
    val profileId: String,
    val name: String,
    val targetState: String,
    val isActive: Boolean,
    val useAutomaticFallback: Boolean,
    val actionsCount: Int,
    val createdAt: String? = null
)