package com.stressless.routes

import com.stressless.dto.app.ChangeOperationalStateRequest
import com.stressless.dto.app.CreateProfileRequest
import com.stressless.dto.app.ManualHubCommandRequest
import com.stressless.dto.app.UpdateProfileActiveRequest
import com.stressless.dto.app.UpdateProfileRequest
import com.stressless.repositories.AppRepository
import com.stressless.security.authenticatedUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.util.UUID

fun Route.appRoutes() {
    get("/api/v1/app/home") {
        call.respond(AppRepository.getHome(call.authenticatedUserId()))
    }

    get("/api/v1/rooms/primary") {
        call.respond(AppRepository.getRoomPrimary(call.authenticatedUserId()))
    }

    get("/api/v1/bands") {
        call.respond(AppRepository.getBands(call.authenticatedUserId()))
    }

    get("/api/v1/profiles") {
        call.respond(AppRepository.getProfiles(call.authenticatedUserId()))
    }

    get("/api/v1/profiles/{profileId}") {
        val profileId = UUID.fromString(
            call.parameters["profileId"] ?: error("profileId is required")
        )

        call.respond(
            AppRepository.getProfileDetail(
                userId = call.authenticatedUserId(),
                profileId = profileId
            )
        )
    }

    post("/api/v1/profiles") {
        val request = call.receive<CreateProfileRequest>()

        call.respond(
            HttpStatusCode.Created,
            AppRepository.createProfile(
                userId = call.authenticatedUserId(),
                request = request
            )
        )
    }

    put("/api/v1/profiles/{profileId}") {
        val profileId = UUID.fromString(
            call.parameters["profileId"] ?: error("profileId is required")
        )

        val request = call.receive<UpdateProfileRequest>()

        call.respond(
            AppRepository.updateProfile(
                userId = call.authenticatedUserId(),
                profileId = profileId,
                request = request
            )
        )
    }

    patch("/api/v1/profiles/{profileId}/active") {
        val profileId = UUID.fromString(
            call.parameters["profileId"] ?: error("profileId is required")
        )

        val request = call.receive<UpdateProfileActiveRequest>()

        call.respond(
            AppRepository.updateProfileActive(
                userId = call.authenticatedUserId(),
                profileId = profileId,
                isActive = request.isActive
            )
        )
    }

    get("/api/v1/stress/recent-events") {
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

        call.respond(
            AppRepository.getRecentEvents(
                userId = call.authenticatedUserId(),
                limit = limit.coerceIn(1, 100)
            )
        )
    }

    post("/api/v1/hubs/{hubLogicalId}/operational-state") {
        val hubLogicalId = call.parameters["hubLogicalId"]
            ?: error("hubLogicalId is required")

        val request = call.receive<ChangeOperationalStateRequest>()

        call.respond(
            AppRepository.changeOperationalState(
                userId = call.authenticatedUserId(),
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
                userId = call.authenticatedUserId(),
                hubLogicalId = hubLogicalId,
                request = request
            )
        )
    }
}