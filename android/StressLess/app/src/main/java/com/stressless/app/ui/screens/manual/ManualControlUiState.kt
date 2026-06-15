package com.stressless.app.ui.screens.manual

data class ManualControlUiState(
    val hubLogicalId: String = "hub-001",

    val ledOn: Boolean = true,
    val ledBrightness: Float = 50f,
    val ledColorHex: String = "#00FFAA",

    val fanOn: Boolean = false,
    val fanSpeed: String = "LOW",

    val lcdMessage: String = "Manual app",

    val buzzerOn: Boolean = false,
    val buzzerVolume: Float = 20f,

    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,

    val lastCommandId: String? = null,
    val commandStatus: String? = null
)