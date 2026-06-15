package com.stressless.app.ui.screens.profiles

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
class ProfilesViewModel @Inject constructor(
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfilesUiState(isLoading = true)

            when (val result = appRepository.getProfiles()) {
                is ApiResult.Success -> {
                    _uiState.value = ProfilesUiState(
                        isLoading = false,
                        profiles = result.data.profiles
                    )
                }

                is ApiResult.Error -> {
                    _uiState.value = ProfilesUiState(
                        isLoading = false,
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}