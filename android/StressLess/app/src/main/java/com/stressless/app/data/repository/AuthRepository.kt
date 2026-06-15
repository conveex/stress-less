package com.stressless.app.data.repository

import com.stressless.app.data.local.SessionPreferences
import com.stressless.app.data.remote.api.AuthApi
import com.stressless.app.data.remote.dto.auth.AuthResponseDto
import com.stressless.app.data.remote.dto.auth.LoginRequestDto
import com.stressless.app.data.remote.dto.auth.RegisterRequestDto
import com.stressless.app.util.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val sessionPreferences: SessionPreferences
) {

    suspend fun login(
        email: String,
        password: String
    ): ApiResult<AuthResponseDto> {
        return try {
            val response = authApi.login(
                LoginRequestDto(
                    email = email.trim(),
                    password = password
                )
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    sessionPreferences.saveSession(
                        token = body.token,
                        userId = body.userId,
                        name = body.name,
                        email = body.email,
                        expiresAt = body.expiresAt
                    )

                    ApiResult.Success(body)
                } else {
                    ApiResult.Error("Respuesta vacía del servidor.")
                }
            } else {
                ApiResult.Error(mapAuthError(response.code()))
            }
        } catch (ex: Exception) {
            ApiResult.Error(
                ex.message ?: "No se pudo conectar con el servidor."
            )
        }
    }

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): ApiResult<AuthResponseDto> {
        return try {
            val response = authApi.register(
                RegisterRequestDto(
                    name = name.trim(),
                    email = email.trim(),
                    password = password
                )
            )

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    sessionPreferences.saveSession(
                        token = body.token,
                        userId = body.userId,
                        name = body.name,
                        email = body.email,
                        expiresAt = body.expiresAt
                    )

                    ApiResult.Success(body)
                } else {
                    ApiResult.Error("Respuesta vacía del servidor.")
                }
            } else {
                ApiResult.Error(mapAuthError(response.code()))
            }
        } catch (ex: Exception) {
            ApiResult.Error(
                ex.message ?: "No se pudo conectar con el servidor."
            )
        }
    }

    suspend fun logout() {
        sessionPreferences.clearSession()
    }

    private fun mapAuthError(code: Int): String {
        return when (code) {
            400 -> "Datos inválidos. Revisa la información ingresada."
            401 -> "Correo o contraseña incorrectos."
            409 -> "Este correo ya está registrado."
            500 -> "Error interno del servidor."
            else -> "Error inesperado. Código: $code"
        }
    }
}