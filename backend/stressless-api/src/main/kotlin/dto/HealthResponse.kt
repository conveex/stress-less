package com.stressless.dto

import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val environment: String
)