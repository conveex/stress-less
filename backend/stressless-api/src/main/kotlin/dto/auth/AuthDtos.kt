package com.stressless.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val userId: String,
    val name: String,
    val email: String? = null,
    val token: String,
    val expiresAt: String
)

@Serializable
data class RefreshTokenResponse(
    val token: String,
    val expiresAt: String
)