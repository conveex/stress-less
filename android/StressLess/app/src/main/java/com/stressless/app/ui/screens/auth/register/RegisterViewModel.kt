package com.stressless.app.ui.screens.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stressless.app.data.repository.AuthRepository
import com.stressless.app.util.ApiResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(
            name = value,
            errorMessage = null
        )
    }

    fun onEmailChange(value: String) {
        _uiState.value = _uiState.value.copy(
            email = value,
            errorMessage = null
        )
    }

    fun onPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            password = value,
            errorMessage = null
        )
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.value = _uiState.value.copy(
            confirmPassword = value,
            errorMessage = null
        )
    }

    fun onAcceptedPrivacyChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            acceptedPrivacy = value,
            errorMessage = null
        )
    }

    fun register() {
        val current = _uiState.value

        if (current.name.isBlank()) {
            _uiState.value = current.copy(
                errorMessage = "Ingresa tu nombre."
            )
            return
        }

        if (!current.email.contains("@")) {
            _uiState.value = current.copy(
                errorMessage = "Ingresa un correo válido."
            )
            return
        }

        if (current.password.length < 8) {
            _uiState.value = current.copy(
                errorMessage = "La contraseña debe tener al menos 8 caracteres."
            )
            return
        }

        if (current.password != current.confirmPassword) {
            _uiState.value = current.copy(
                errorMessage = "Las contraseñas no coinciden."
            )
            return
        }

        if (!current.acceptedPrivacy) {
            _uiState.value = current.copy(
                errorMessage = "Debes aceptar el aviso de privacidad."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(
                isLoading = true,
                errorMessage = null
            )

            when (
                val result = authRepository.register(
                    name = current.name,
                    email = current.email,
                    password = current.password
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registerSuccess = true,
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