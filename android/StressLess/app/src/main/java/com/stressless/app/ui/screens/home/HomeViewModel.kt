package com.stressless.app.ui.screens.home

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
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(isLoading = true)

            when (val result = appRepository.getHome()) {
                is ApiResult.Success -> {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        data = result.data
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}