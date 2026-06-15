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

    fun changeMode(mode: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                selectedMode = mode,
                isLoading = true,
                message = null,
                errorMessage = null
            )

            when (
                val result = appRepository.changeOperationalState(
                    hubLogicalId = _uiState.value.hubLogicalId,
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
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}