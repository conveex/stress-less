package com.stressless.config

object AppConfig {
    val environment: String = env("APP_ENV", "development")
    val serviceName: String = env("APP_NAME", "stressless-api")
    val version: String = env("APP_VERSION", "0.1.0")

    fun env(name: String, default: String): String {
        return System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
    }

    fun envRequired(name: String): String {
        return System.getenv(name)?.takeIf { it.isNotBlank() }
            ?: error("Missing required environment variable: $name")
    }
}