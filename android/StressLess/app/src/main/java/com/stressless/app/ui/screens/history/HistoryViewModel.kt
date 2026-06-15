package com.stressless.app.ui.screens.history

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
class HistoryViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = HistoryUiState(isLoading = true)

            when (val result = appRepository.getRecentEvents(limit = 20)) {
                is ApiResult.Success -> {
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        events = result.data.events
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = HistoryUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}