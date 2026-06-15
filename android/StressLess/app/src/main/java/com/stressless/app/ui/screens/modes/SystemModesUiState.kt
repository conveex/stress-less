package com.stressless.app.ui.screens.modes

data class SystemModesUiState(
    val selectedMode: String = "ACTIVE",
    val hubLogicalId: String = "hub-001",
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null
)