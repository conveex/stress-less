package com.stressless.app.ui.screens.home

import com.stressless.app.data.remote.dto.app.AppHomeResponseDto

data class HomeUiState(
    val isLoading: Boolean = true,
    val data: AppHomeResponseDto? = null,
    val errorMessage: String? = null
)