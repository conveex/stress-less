package com.stressless.plugins

import com.stressless.dto.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import java.time.Instant

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error = "BAD_REQUEST",
                    message = cause.message ?: "Invalid request",
                    timestamp = Instant.now().toString()
                )
            )
        }

        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)

            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = "INTERNAL_SERVER_ERROR",
                    message = "Unexpected server error",
                    timestamp = Instant.now().toString()
                )
            )
        }
    }
}