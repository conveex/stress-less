package com.stressless.app.ui.screens.profiles

import com.stressless.app.data.remote.dto.app.ProfileResponseDto

data class ProfilesUiState(
    val isLoading: Boolean = true,
    val profiles: List<ProfileResponseDto> = emptyList(),
    val errorMessage: String? = null
)