package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.DbHealthResponse

object DbHealthRepository {

    fun check(): DbHealthResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val databaseName = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT current_database()").use { rs ->
                    rs.next()
                    rs.getString(1)
                }
            }

            val currentUser = connection.createStatement().use { statement ->
                statement.executeQuery("SELECT current_user").use { rs ->
                    rs.next()
                    rs.getString(1)
                }
            }

            return DbHealthResponse(
                status = "OK",
                database = databaseName,
                user = currentUser,
                usersCount = countTable(connection, "users"),
                hubsCount = countTable(connection, "hubs"),
                bandsCount = countTable(connection, "bands"),
                devicesCount = countTable(connection, "devices")
            )
        }
    }

    private fun countTable(connection: java.sql.Connection, tableName: String): Long {
        connection.createStatement().use { statement ->
            statement.executeQuery("SELECT COUNT(*) FROM $tableName").use { rs ->
                rs.next()
                return rs.getLong(1)
            }
        }
    }
}