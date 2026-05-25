package com.stressless.routes

import com.stressless.repositories.DbHealthRepository
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.devRoutes() {
    get("/api/v1/dev/db-health") {
        call.respond(DbHealthRepository.check())
    }
}