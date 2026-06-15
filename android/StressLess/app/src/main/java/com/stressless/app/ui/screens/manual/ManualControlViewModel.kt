package com.stressless.app.ui.screens.manual

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stressless.app.data.remote.dto.app.ManualHubActionRequestDto
import com.stressless.app.data.remote.dto.app.ManualHubCommandRequestDto
import com.stressless.app.data.repository.AppRepository
import com.stressless.app.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

@HiltViewModel
class ManualControlViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManualControlUiState())
    val uiState: StateFlow<ManualControlUiState> = _uiState.asStateFlow()

    fun onLedOnChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(ledOn = value)
    }

    fun onLedBrightnessChange(value: Float) {
        _uiState.value = _uiState.value.copy(ledBrightness = value)
    }

    fun onLedColorChange(value: String) {
        _uiState.value = _uiState.value.copy(ledColorHex = value)
    }

    fun onFanOnChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(fanOn = value)
    }

    fun onFanSpeedChange(value: String) {
        _uiState.value = _uiState.value.copy(fanSpeed = value)
    }

    fun onLcdMessageChange(value: String) {
        _uiState.value = _uiState.value.copy(
            lcdMessage = value.take(32)
        )
    }

    fun onBuzzerOnChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(buzzerOn = value)
    }

    fun onBuzzerVolumeChange(value: Float) {
        _uiState.value = _uiState.value.copy(buzzerVolume = value)
    }

    fun sendLedCommand() {
        val state = _uiState.value

        val actions = mutableListOf<ManualHubActionRequestDto>()

        actions.add(
            ManualHubActionRequestDto(
                deviceKey = "led-rgb-001",
                action = if (state.ledOn) "TURN_ON" else "TURN_OFF",
                value = JsonPrimitive(state.ledOn)
            )
        )

        if (state.ledOn) {
            actions.add(
                ManualHubActionRequestDto(
                    deviceKey = "led-rgb-001",
                    action = "SET_BRIGHTNESS",
                    value = JsonPrimitive(state.ledBrightness.toInt())
                )
            )

            actions.add(
                ManualHubActionRequestDto(
                    deviceKey = "led-rgb-001",
                    action = "SET_COLOR_HEX",
                    value = JsonPrimitive(state.ledColorHex)
                )
            )
        }

        sendActions(actions)
    }

    fun sendFanCommand() {
        val state = _uiState.value

        val actions = mutableListOf<ManualHubActionRequestDto>()

        actions.add(
            ManualHubActionRequestDto(
                deviceKey = "fan-001",
                action = if (state.fanOn) "TURN_ON" else "TURN_OFF",
                value = JsonPrimitive(state.fanOn)
            )
        )

        if (state.fanOn) {
            actions.add(
                ManualHubActionRequestDto(
                    deviceKey = "fan-001",
                    action = "SET_SPEED",
                    value = JsonPrimitive(state.fanSpeed)
                )
            )
        }

        sendActions(actions)
    }

    fun sendLcdCommand() {
        val state = _uiState.value

        sendActions(
            listOf(
                ManualHubActionRequestDto(
                    deviceKey = "display-001",
                    action = "SHOW_MESSAGE",
                    value = JsonPrimitive(state.lcdMessage)
                )
            )
        )
    }

    fun sendBuzzerCommand() {
        val state = _uiState.value

        val actions = mutableListOf<ManualHubActionRequestDto>()

        actions.add(
            ManualHubActionRequestDto(
                deviceKey = "buzzer-001",
                action = if (state.buzzerOn) "TURN_ON" else "TURN_OFF",
                value = JsonPrimitive(state.buzzerOn)
            )
        )

        if (state.buzzerOn) {
            actions.add(
                ManualHubActionRequestDto(
                    deviceKey = "buzzer-001",
                    action = "SET_VOLUME",
                    value = JsonPrimitive(state.buzzerVolume.toInt())
                )
            )
        }

        sendActions(actions)
    }

    fun sendAllCommand() {
        val state = _uiState.value

        val actions = buildList {
            add(
                ManualHubActionRequestDto(
                    deviceKey = "led-rgb-001",
                    action = if (state.ledOn) "TURN_ON" else "TURN_OFF",
                    value = JsonPrimitive(state.ledOn)
                )
            )

            if (state.ledOn) {
                add(
                    ManualHubActionRequestDto(
                        deviceKey = "led-rgb-001",
                        action = "SET_BRIGHTNESS",
                        value = JsonPrimitive(state.ledBrightness.toInt())
                    )
                )

                add(
                    ManualHubActionRequestDto(
                        deviceKey = "led-rgb-001",
                        action = "SET_COLOR_HEX",
                        value = JsonPrimitive(state.ledColorHex)
                    )
                )
            }

            add(
                ManualHubActionRequestDto(
                    deviceKey = "fan-001",
                    action = if (state.fanOn) "TURN_ON" else "TURN_OFF",
                    value = JsonPrimitive(state.fanOn)
                )
            )

            if (state.fanOn) {
                add(
                    ManualHubActionRequestDto(
                        deviceKey = "fan-001",
                        action = "SET_SPEED",
                        value = JsonPrimitive(state.fanSpeed)
                    )
                )
            }

            add(
                ManualHubActionRequestDto(
                    deviceKey = "display-001",
                    action = "SHOW_MESSAGE",
                    value = JsonPrimitive(state.lcdMessage)
                )
            )

            add(
                ManualHubActionRequestDto(
                    deviceKey = "buzzer-001",
                    action = if (state.buzzerOn) "TURN_ON" else "TURN_OFF",
                    value = JsonPrimitive(state.buzzerOn)
                )
            )

            if (state.buzzerOn) {
                add(
                    ManualHubActionRequestDto(
                        deviceKey = "buzzer-001",
                        action = "SET_VOLUME",
                        value = JsonPrimitive(state.buzzerVolume.toInt())
                    )
                )
            }
        }

        sendActions(actions)
    }

    private fun sendActions(actions: List<ManualHubActionRequestDto>) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(
                isLoading = true,
                message = null,
                errorMessage = null
            )

            when (
                val result = appRepository.sendManualCommand(
                    hubLogicalId = state.hubLogicalId,
                    request = ManualHubCommandRequestDto(actions)
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Comando enviado: ${result.data.commandId}",
                        errorMessage = null
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}