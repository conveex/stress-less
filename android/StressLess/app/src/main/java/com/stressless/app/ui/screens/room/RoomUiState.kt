package com.stressless.app.ui.screens.room

import com.stressless.app.data.remote.dto.app.RoomPrimaryResponseDto

data class RoomUiState(
    val isLoading: Boolean = true,
    val data: RoomPrimaryResponseDto? = null,
    val errorMessage: String? = null
)