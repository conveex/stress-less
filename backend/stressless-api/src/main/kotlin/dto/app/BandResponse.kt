package com.stressless.dto.app

import kotlinx.serialization.Serializable

@Serializable
data class BandsResponse(
    val bands: List<BandResponse>
)

@Serializable
data class BandResponse(
    val bandId: String,
    val bandLogicalId: String,
    val serialNumber: String?,
    val isActive: Boolean,
    val status: String,
    val batteryLevel: Int?,
    val lastSeenAt: String?,
    val createdAt: String
)