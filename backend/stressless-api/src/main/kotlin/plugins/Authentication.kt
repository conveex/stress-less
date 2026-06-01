package com.stressless.plugins

import com.stressless.config.AppConfig
import com.stressless.security.JwtService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

fun Application.configureAuthentication() {
    install(Authentication) {
        jwt("jwt") {
            realm = AppConfig.jwtRealm

            verifier(JwtService.verifier())

            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val email = credential.payload.getClaim("email").asString()

                if (!userId.isNullOrBlank() && !email.isNullOrBlank()) {
                    io.ktor.server.auth.jwt.JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}