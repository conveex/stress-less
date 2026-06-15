package com.stressless.app.data.remote.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val userId: String,
    val name: String,
    val email: String? = null,
    val token: String,
    val expiresAt: String
)

@Serializable
data class RefreshTokenResponseDto(
    val token: String,
    val expiresAt: String
)