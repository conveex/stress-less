package com.stressless.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommandSummaryResponse(
    val id: String,
    val commandId: String?,
    val source: String,
    val status: String,
    val sentAt: String,
    val acknowledgedAt: String?,
    val payloadPreview: String
)