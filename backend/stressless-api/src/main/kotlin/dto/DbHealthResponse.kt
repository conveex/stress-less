package com.stressless.dto

import kotlinx.serialization.Serializable

@Serializable
data class DbHealthResponse(
    val status: String,
    val database: String,
    val user: String,
    val usersCount: Long,
    val hubsCount: Long,
    val bandsCount: Long,
    val devicesCount: Long
)
