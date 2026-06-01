package com.stressless.security

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import java.util.UUID

fun ApplicationCall.authenticatedUserId(): UUID {
    val principal = principal<JWTPrincipal>()
        ?: error("UNAUTHORIZED")

    val userId = principal.payload.getClaim("userId").asString()
        ?: error("UNAUTHORIZED")

    return UUID.fromString(userId)
}

fun ApplicationCall.authenticatedEmail(): String {
    val principal = principal<JWTPrincipal>()
        ?: error("UNAUTHORIZED")

    return principal.payload.getClaim("email").asString()
        ?: error("UNAUTHORIZED")
}