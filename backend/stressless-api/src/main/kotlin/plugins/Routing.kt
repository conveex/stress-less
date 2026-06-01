package com.stressless.plugins

import com.stressless.routes.appRoutes
import com.stressless.routes.authRoutes
import com.stressless.routes.devRoutes
import com.stressless.routes.healthRoutes
import com.stressless.routes.stressRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing

fun Application.configureRouting() {
    routing {
        healthRoutes()
        authRoutes()

        // Endpoints temporales de desarrollo abiertos.
        devRoutes()

        // Endpoints reales de app protegidos con JWT.
        authenticate("jwt") {
            appRoutes()
            stressRoutes()
        }
    }
}