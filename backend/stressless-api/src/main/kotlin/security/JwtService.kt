package com.stressless.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.stressless.config.AppConfig
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

object JwtService {

    private val algorithm = Algorithm.HMAC256(AppConfig.jwtSecret)

    data class TokenResult(
        val token: String,
        val expiresAt: Instant
    )

    fun generateToken(
        userId: UUID,
        email: String
    ): TokenResult {
        val expiresAt = Instant.now().plus(AppConfig.jwtExpirationHours, ChronoUnit.HOURS)

        val token = JWT.create()
            .withIssuer(AppConfig.jwtIssuer)
            .withAudience(AppConfig.jwtAudience)
            .withSubject(userId.toString())
            .withClaim("userId", userId.toString())
            .withClaim("email", email)
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)

        return TokenResult(
            token = token,
            expiresAt = expiresAt
        )
    }

    fun verifier() = JWT
        .require(algorithm)
        .withIssuer(AppConfig.jwtIssuer)
        .withAudience(AppConfig.jwtAudience)
        .build()
}