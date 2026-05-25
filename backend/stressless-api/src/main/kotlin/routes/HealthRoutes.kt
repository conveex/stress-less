package com.stressless.routes

import com.stressless.config.AppConfig
import com.stressless.dto.HealthResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoutes() {
    get("/") {
        call.respond(
            HealthResponse(
                status = "OK",
                service = AppConfig.serviceName,
                version = AppConfig.version,
                environment = AppConfig.environment
            )
        )
    }

    get("/api/v1/health") {
        call.respond(
            HealthResponse(
                status = "OK",
                service = AppConfig.serviceName,
                version = AppConfig.version,
                environment = AppConfig.environment
            )
        )
    }
}