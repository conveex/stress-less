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

    val mqttHost: String = envRequired("MQTT_HOST")
    val mqttPort: Int = env("MQTT_PORT", "8883").toInt()
    val mqttUsername: String = envRequired("MQTT_USERNAME")
    val mqttPassword: String = envRequired("MQTT_PASSWORD")
    val mqttClientId: String = env("MQTT_CLIENT_ID", "backend-stressless")
    val mqttTls: Boolean = env("MQTT_TLS", "true").toBoolean()

    val jwtSecret: String = envRequired("JWT_SECRET")
    val jwtIssuer: String = env("JWT_ISSUER", "stressless-api")
    val jwtAudience: String = env("JWT_AUDIENCE", "stressless-app")
    val jwtRealm: String = env("JWT_REALM", "stressless")
    val jwtExpirationHours: Long = env("JWT_EXPIRATION_HOURS", "24").toLong()

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