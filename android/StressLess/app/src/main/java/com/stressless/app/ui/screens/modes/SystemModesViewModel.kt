package com.stressless.app.ui.screens.modes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stressless.app.data.repository.AppRepository
import com.stressless.app.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SystemModesViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SystemModesUiState())
    val uiState: StateFlow<SystemModesUiState> = _uiState.asStateFlow()

    init {
        loadCurrentMode()
    }

    fun loadCurrentMode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                message = null,
                errorMessage = null
            )

            when (val result = appRepository.getHome()) {
                is ApiResult.Success -> {
                    val hub = result.data.hub

                    _uiState.value = _uiState.value.copy(
                        hubLogicalId = hub?.hubLogicalId ?: "hub-001",
                        selectedMode = hub?.operationalState ?: "ACTIVE",
                        isLoading = false
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

    fun changeMode(mode: String) {
        viewModelScope.launch {
            val current = _uiState.value

            _uiState.value = current.copy(
                selectedMode = mode,
                isLoading = true,
                message = null,
                errorMessage = null
            )

            when (
                val result = appRepository.changeOperationalState(
                    hubLogicalId = current.hubLogicalId,
                    state = mode
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        selectedMode = result.data.newState,
                        isLoading = false,
                        message = "Modo cambiado a ${result.data.newState}.",
                        errorMessage = null
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        selectedMode = current.selectedMode,
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}