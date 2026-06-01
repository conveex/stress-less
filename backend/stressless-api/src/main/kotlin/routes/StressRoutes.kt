package com.stressless.routes

import com.stressless.repositories.StressRepository
import com.stressless.security.authenticatedUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.stressRoutes() {
    get("/api/v1/stress/current") {
        val response = StressRepository.getCurrentForUser(call.authenticatedUserId())

        if (response == null) {
            call.respond(
                HttpStatusCode.NotFound,
                mapOf(
                    "error" to "NO_STRESS_STATE",
                    "message" to "No detected stress state found for demo user"
                )
            )
            return@get
        }

        call.respond(response)
    }
}