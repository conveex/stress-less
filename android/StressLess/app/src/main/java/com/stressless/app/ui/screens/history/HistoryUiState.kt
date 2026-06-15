package com.stressless.app.ui.screens.history

import com.stressless.app.data.remote.dto.app.StressRecentEventResponseDto

data class HistoryUiState(
    val isLoading: Boolean = true,
    val events: List<StressRecentEventResponseDto> = emptyList(),
    val errorMessage: String? = null
)