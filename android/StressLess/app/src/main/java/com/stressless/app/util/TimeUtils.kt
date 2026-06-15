package com.stressless.app.util

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

fun isRecentlySeen(
    isoDate: String?,
    maxAgeSeconds: Long = 120
): Boolean {
    if (isoDate.isNullOrBlank()) return false

    return try {
        val instant = Instant.parse(isoDate)

        // Evita considerar válidas fechas basura como 1970 del ESP32.
        if (instant.isBefore(Instant.parse("2025-01-01T00:00:00Z"))) {
            return false
        }

        val age = Duration.between(instant, Instant.now()).seconds
        age in 0..maxAgeSeconds
    } catch (ex: DateTimeParseException) {
        false
    } catch (ex: Exception) {
        false
    }
}

fun isHubEffectivelyOnline(
    status: String?,
    lastSeenAt: String?,
    ipAddress: String?
): Boolean {
    return isRecentlySeen(lastSeenAt) ||
            (status == "ACTIVE" && !ipAddress.isNullOrBlank())
}

fun relativeSeenText(
    isoDate: String?
): String {
    if (isoDate.isNullOrBlank()) return "Sin conexión reciente"

    return try {
        val instant = Instant.parse(isoDate)

        if (instant.isBefore(Instant.parse("2025-01-01T00:00:00Z"))) {
            return "Timestamp no confiable del dispositivo"
        }

        val seconds = Duration.between(instant, Instant.now()).seconds

        when {
            seconds < 0 -> "Hace unos segundos"
            seconds < 60 -> "Hace ${seconds}s"
            seconds < 3600 -> "Hace ${seconds / 60} min"
            else -> "Hace ${seconds / 3600} h"
        }
    } catch (ex: Exception) {
        "Fecha no disponible"
    }
}