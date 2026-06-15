package com.stressless.app.ui.screens.room

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
class RoomViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    init {
        loadRoom()
    }

    fun loadRoom() {
        viewModelScope.launch {
            _uiState.value = RoomUiState(isLoading = true)

            when (val result = appRepository.getPrimaryRoom()) {
                is ApiResult.Success -> {
                    _uiState.value = RoomUiState(
                        isLoading = false,
                        data = result.data
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = RoomUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}