package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.CommandSummaryResponse

object CommandRepository {

    fun acknowledgeCommandByPayloadCommandId(commandId: String): Int {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                UPDATE commands
                SET status = 'ACKNOWLEDGED',
                    acknowledged_at = NOW()
                WHERE payload ->> 'commandId' = ?
                  AND status = 'SENT'
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, commandId)
                return statement.executeUpdate()
            }
        }
    }

    fun failCommandByPayloadCommandId(commandId: String): Int {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                UPDATE commands
                SET status = 'FAILED',
                    acknowledged_at = NOW()
                WHERE payload ->> 'commandId' = ?
                  AND status = 'SENT'
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, commandId)
                return statement.executeUpdate()
            }
        }
    }

    fun listRecent(limit: Int = 10): List<CommandSummaryResponse> {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val sql = """
                SELECT
                    id,
                    payload ->> 'commandId' AS command_id,
                    source::text AS source,
                    status::text AS status,
                    sent_at,
                    acknowledged_at,
                    LEFT(payload::text, 500) AS payload_preview
                FROM commands
                ORDER BY sent_at DESC
                LIMIT ?
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, limit)

                statement.executeQuery().use { rs ->
                    val result = mutableListOf<CommandSummaryResponse>()

                    while (rs.next()) {
                        result.add(
                            CommandSummaryResponse(
                                id = rs.getString("id"),
                                commandId = rs.getString("command_id"),
                                source = rs.getString("source"),
                                status = rs.getString("status"),
                                sentAt = rs.getTimestamp("sent_at").toInstant().toString(),
                                acknowledgedAt = rs.getTimestamp("acknowledged_at")?.toInstant()?.toString(),
                                payloadPreview = rs.getString("payload_preview")
                            )
                        )
                    }

                    return result
                }
            }
        }
    }
}