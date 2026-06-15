package com.stressless.app.data.remote.api

import com.stressless.app.data.remote.dto.auth.AuthResponseDto
import com.stressless.app.data.remote.dto.auth.LoginRequestDto
import com.stressless.app.data.remote.dto.auth.RefreshTokenResponseDto
import com.stressless.app.data.remote.dto.auth.RegisterRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<AuthResponseDto>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<AuthResponseDto>

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(): Response<RefreshTokenResponseDto>
}