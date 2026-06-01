package com.stressless.routes

import com.stressless.dto.app.ChangeOperationalStateRequest
import com.stressless.dto.app.ManualHubCommandRequest
import com.stressless.repositories.AppRepository
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.appRoutes() {
    get("/api/v1/app/home") {
        call.respond(AppRepository.getHome())
    }

    get("/api/v1/rooms/primary") {
        call.respond(AppRepository.getRoomPrimary())
    }

    get("/api/v1/bands") {
        call.respond(AppRepository.getBands())
    }

    get("/api/v1/profiles") {
        call.respond(AppRepository.getProfiles())
    }

    get("/api/v1/stress/recent-events") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        call.respond(AppRepository.getRecentEvents(limit.coerceIn(1, 100)))
    }

    post("/api/v1/hubs/{hubLogicalId}/operational-state") {
        val hubLogicalId = call.parameters["hubLogicalId"]
            ?: error("hubLogicalId is required")

        val request = call.receive<ChangeOperationalStateRequest>()

        call.respond(
            AppRepository.changeOperationalState(
                hubLogicalId = hubLogicalId,
                newState = request.state
            )
        )
    }

    post("/api/v1/hubs/{hubLogicalId}/commands") {
        val hubLogicalId = call.parameters["hubLogicalId"]
            ?: error("hubLogicalId is required")

        val request = call.receive<ManualHubCommandRequest>()

        call.respond(
            AppRepository.sendManualCommand(
                hubLogicalId = hubLogicalId,
                request = request
            )
        )
    }
}