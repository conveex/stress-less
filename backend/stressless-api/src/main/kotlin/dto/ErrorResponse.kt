package com.stressless.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String
)