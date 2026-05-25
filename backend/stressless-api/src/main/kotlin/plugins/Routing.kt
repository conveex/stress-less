package com.stressless.plugins

import com.stressless.routes.devRoutes
import com.stressless.routes.healthRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        healthRoutes()
        devRoutes()
    }
}