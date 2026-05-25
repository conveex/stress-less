package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.mqtt.BiometricsPayload
import com.stressless.dto.stress.StressClassificationResult
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

object BiometricsRepository {

    data class BandHubContext(
        val userId: UUID,
        val bandUuid: UUID,
        val hubUuid: UUID,
        val roomUuid: UUID,
        val baselineBpm: Double,
        val baselineGsr: Double,
        val baselineMovement: Double
    )

    fun findContext(payload: BiometricsPayload): BandHubContext? {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT
                    u.id AS user_id,
                    b.id AS band_uuid,
                    h.id AS hub_uuid,
                    r.id AS room_uuid,
                    u.baseline_bpm,
                    u.baseline_gsr,
                    u.baseline_movement
                FROM bands b
                JOIN users u ON u.id = b.user_id
                JOIN rooms r ON r.user_id = u.id AND r.is_primary = true
                JOIN hubs h ON h.room_id = r.id
                WHERE b.band_id = ?
                  AND h.hub_id = ?
                  AND b.is_active = true
                  AND u.deleted_at IS NULL
                LIMIT 1
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, payload.bandId)
                statement.setString(2, payload.hubId)

                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return null
                    }

                    return BandHubContext(
                        userId = UUID.fromString(rs.getString("user_id")),
                        bandUuid = UUID.fromString(rs.getString("band_uuid")),
                        hubUuid = UUID.fromString(rs.getString("hub_uuid")),
                        roomUuid = UUID.fromString(rs.getString("room_uuid")),
                        baselineBpm = rs.getDouble("baseline_bpm"),
                        baselineGsr = rs.getDouble("baseline_gsr"),
                        baselineMovement = rs.getDouble("baseline_movement")
                    )
                }
            }
        }
    }

    fun saveBiometricEvent(
        payload: BiometricsPayload,
        context: BandHubContext
    ): UUID {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                INSERT INTO biometric_events (
                    band_id,
                    hub_id,
                    user_id,
                    bpm,
                    gsr,
                    movement,
                    battery,
                    source,
                    timestamp,
                    received_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::biometric_source_enum, ?, NOW())
                RETURNING id
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, context.bandUuid)
                statement.setObject(2, context.hubUuid)
                statement.setObject(3, context.userId)
                statement.setDouble(4, payload.bpm)
                statement.setDouble(5, payload.gsr)
                statement.setDouble(6, payload.movement)

                if (payload.battery != null) {
                    statement.setInt(7, payload.battery)
                } else {
                    statement.setNull(7, java.sql.Types.INTEGER)
                }

                statement.setString(8, payload.source)

                val effectiveTimestamp = parseTimestampOrNow(payload.timestamp)
                statement.setTimestamp(9, Timestamp.from(effectiveTimestamp))

                statement.executeQuery().use { rs ->
                    rs.next()
                    return UUID.fromString(rs.getString("id"))
                }
            }
        }
    }

    fun updateBandLastSeen(payload: BiometricsPayload) {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                UPDATE bands
                SET status = 'CONNECTED',
                    battery_level = ?,
                    last_seen_at = NOW()
                WHERE band_id = ?
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                if (payload.battery != null) {
                    statement.setInt(1, payload.battery)
                } else {
                    statement.setNull(1, java.sql.Types.INTEGER)
                }

                statement.setString(2, payload.bandId)
                statement.executeUpdate()
            }
        }
    }

    fun saveDetectedState(
        context: BandHubContext,
        classification: StressClassificationResult,
        profileApplied: UUID? = null
    ): UUID {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                INSERT INTO detected_states (
                    user_id,
                    hub_id,
                    state,
                    confidence,
                    bpm_delta,
                    gsr_delta,
                    movement_at_detection,
                    reason,
                    profile_applied,
                    detected_at
                )
                VALUES (?, ?, ?::physiological_state_enum, ?, ?, ?, ?, ?::jsonb, ?, NOW())
                RETURNING id
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, context.userId)
                statement.setObject(2, context.hubUuid)
                statement.setString(3, classification.state)
                statement.setDouble(4, classification.confidence)
                statement.setDouble(5, classification.bpmDelta)
                statement.setDouble(6, classification.gsrDelta)
                statement.setDouble(7, classification.movementAtDetection)
                statement.setString(8, classification.reasonJson)

                if (profileApplied != null) {
                    statement.setObject(9, profileApplied)
                } else {
                    statement.setNull(9, java.sql.Types.OTHER)
                }

                statement.executeQuery().use { rs ->
                    rs.next()
                    return UUID.fromString(rs.getString("id"))
                }
            }
        }
    }

    fun findActiveProfileId(
        userId: UUID,
        state: String
    ): UUID? {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT id
                FROM environment_profiles
                WHERE user_id = ?
                  AND target_state = ?::physiological_state_enum
                  AND is_active = true
                LIMIT 1
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, userId)
                statement.setString(2, state)

                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return null
                    }

                    return UUID.fromString(rs.getString("id"))
                }
            }
        }
    }

    fun loadProfileCommandActions(
        profileId: UUID
    ): String {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT
                    d.device_key,
                    pa.action::text AS action,
                    pa.value::text AS value
                FROM profile_actions pa
                JOIN devices d ON d.id = pa.device_id
                WHERE pa.profile_id = ?
                ORDER BY pa.order_index ASC
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, profileId)

                statement.executeQuery().use { rs ->
                    val actions = mutableListOf<String>()

                    while (rs.next()) {
                        val deviceKey = rs.getString("device_key")
                        val action = rs.getString("action")
                        val rawValue = rs.getString("value")

                        actions.add(
                            """
                            {
                              "deviceKey": "$deviceKey",
                              "action": "$action",
                              "value": $rawValue
                            }
                            """.trimIndent()
                        )
                    }

                    return actions.joinToString(
                        separator = ",\n",
                        prefix = "[\n",
                        postfix = "\n]"
                    )
                }
            }
        }
    }

    fun saveCommand(
        context: BandHubContext,
        source: String,
        detectedStateId: UUID?,
        payload: String
    ): UUID {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                INSERT INTO commands (
                    hub_id,
                    room_id,
                    user_id,
                    source,
                    triggered_by_state,
                    payload,
                    status,
                    sent_at
                )
                VALUES (?, ?, ?, ?::command_source_enum, ?, ?::jsonb, 'SENT', NOW())
                RETURNING id
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, context.hubUuid)
                statement.setObject(2, context.roomUuid)
                statement.setObject(3, context.userId)
                statement.setString(4, source)

                if (detectedStateId != null) {
                    statement.setObject(5, detectedStateId)
                } else {
                    statement.setNull(5, java.sql.Types.OTHER)
                }

                statement.setString(6, payload)

                statement.executeQuery().use { rs ->
                    rs.next()
                    return UUID.fromString(rs.getString("id"))
                }
            }
        }
    }

    private fun parseTimestampOrNow(rawTimestamp: String): Instant {
        return try {
            val parsed = Instant.parse(rawTimestamp)

            if (parsed == Instant.EPOCH) {
                Instant.now()
            } else {
                parsed
            }
        } catch (ex: Exception) {
            Instant.now()
        }
    }
}