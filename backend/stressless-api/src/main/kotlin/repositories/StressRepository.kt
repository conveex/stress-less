package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.stress.CurrentStressResponse

object StressRepository {

    fun getCurrentForUser(userId: java.util.UUID): CurrentStressResponse? {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT
                    user_id,
                    state::text AS state,
                    confidence,
                    bpm_delta,
                    gsr_delta,
                    movement_at_detection,
                    reason::text AS reason,
                    detected_at
                FROM detected_states
                WHERE user_id = ?
                ORDER BY detected_at DESC
                LIMIT 1
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, userId)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) {
                        return null
                    }

                    return CurrentStressResponse(
                        userId = rs.getString("user_id"),
                        detectedState = rs.getString("state"),
                        confidence = rs.getDouble("confidence"),
                        bpmDelta = rs.getDouble("bpm_delta"),
                        gsrDelta = rs.getDouble("gsr_delta"),
                        movementAtDetection = rs.getDouble("movement_at_detection"),
                        reason = rs.getString("reason"),
                        detectedAt = rs.getTimestamp("detected_at")?.toInstant()?.toString()
                    )
                }
            }
        }
    }
}