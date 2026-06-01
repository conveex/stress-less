package com.stressless.routes

import com.stressless.dto.auth.LoginRequest
import com.stressless.dto.auth.RegisterRequest
import com.stressless.repositories.AuthRepository
import com.stressless.security.authenticatedEmail
import com.stressless.security.authenticatedUserId
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.authRoutes() {
    post("/api/v1/auth/register") {
        val request = call.receive<RegisterRequest>()
        call.respond(
            HttpStatusCode.Created,
            AuthRepository.register(request)
        )
    }

    post("/api/v1/auth/login") {
        val request = call.receive<LoginRequest>()
        call.respond(AuthRepository.login(request))
    }

    authenticate("jwt") {
        post("/api/v1/auth/refresh") {
            call.respond(
                AuthRepository.refresh(
                    userId = call.authenticatedUserId(),
                    email = call.authenticatedEmail()
                )
            )
        }
    }
}