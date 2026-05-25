package com.stressless.config

import io.github.cdimascio.dotenv.dotenv

object AppConfig {
    private val dotenv = dotenv {
        ignoreIfMissing = true
    }

    val environment: String = env("APP_ENV", "development")
    val serviceName: String = env("APP_NAME", "stressless-api")
    val version: String = env("APP_VERSION", "0.1.0")

    val databaseHost: String = envRequired("DATABASE_HOST")
    val databasePort: Int = env("DATABASE_PORT", "5432").toInt()
    val databaseName: String = envRequired("DATABASE_NAME")
    val databaseUser: String = envRequired("DATABASE_USER")
    val databasePassword: String = envRequired("DATABASE_PASSWORD")
    val databaseSsl: Boolean = env("DATABASE_SSL", "true").toBoolean()

    val jdbcUrl: String
        get() {
            val sslMode = if (databaseSsl) "require" else "disable"
            return "jdbc:postgresql://$databaseHost:$databasePort/$databaseName?sslmode=$sslMode"
        }

    fun env(name: String, default: String): String {
        return System.getenv(name)
            ?: dotenv[name]
            ?: default
    }

    fun envRequired(name: String): String {
        return System.getenv(name)
            ?: dotenv[name]
            ?: error("Missing required environment variable: $name")
    }
}