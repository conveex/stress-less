package com.stressless.db

import com.stressless.config.AppConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    private lateinit var dataSource: HikariDataSource

    fun init() {
        val config = HikariConfig().apply {
            jdbcUrl = AppConfig.jdbcUrl
            username = AppConfig.databaseUser
            password = AppConfig.databasePassword
            driverClassName = "org.postgresql.Driver"

            maximumPoolSize = 5
            minimumIdle = 1
            connectionTimeout = 10_000
            idleTimeout = 60_000
            maxLifetime = 300_000

            validate()
        }

        dataSource = HikariDataSource(config)
        Database.connect(dataSource)
    }

    fun getDataSource(): HikariDataSource {
        if (!::dataSource.isInitialized) {
            error("DatabaseFactory has not been initialized")
        }
        return dataSource
    }

    fun close() {
        if (::dataSource.isInitialized) {
            dataSource.close()
        }
    }
}