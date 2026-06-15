package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.mqtt.HubStatusPayload

object HubRepository {

    fun updateHubStatus(payload: HubStatusPayload): Int {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                UPDATE hubs
                SET status = ?::hub_status_enum,
                    firmware_version = COALESCE(?, firmware_version),
                    ip_address = COALESCE(?, ip_address),
                    last_seen_at = NOW(),
                    updated_at = NOW()
                WHERE hub_id = ?
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, normalizeHubStatus(payload.status))
                statement.setString(2, payload.firmwareVersion)
                statement.setString(3, payload.ipAddress)
                statement.setString(4, payload.hubId)

                return statement.executeUpdate()
            }
        }
    }

    private fun normalizeHubStatus(status: String): String {
        return when (status.uppercase()) {
            "ACTIVE", "CONNECTED", "ONLINE" -> "ACTIVE"
            "OFFLINE", "DISCONNECTED" -> "OFFLINE"
            "ERROR" -> "ERROR"
            "PENDING" -> "PENDING"
            else -> "ACTIVE"
        }
    }
}