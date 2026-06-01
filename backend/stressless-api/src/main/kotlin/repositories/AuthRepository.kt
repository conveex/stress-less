package com.stressless.repositories

import com.stressless.db.DatabaseFactory
import com.stressless.dto.auth.AuthResponse
import com.stressless.dto.auth.LoginRequest
import com.stressless.dto.auth.RegisterRequest
import com.stressless.security.JwtService
import com.stressless.security.PasswordService
import java.util.UUID

object AuthRepository {

    fun register(request: RegisterRequest): AuthResponse {
        validateRegisterRequest(request)

        DatabaseFactory.getDataSource().connection.use { connection ->
            val email = request.email.trim().lowercase()

            val existsSql = """
                SELECT COUNT(*)
                FROM users
                WHERE email = ?
                  AND deleted_at IS NULL
            """.trimIndent()

            connection.prepareStatement(existsSql).use { st ->
                st.setString(1, email)

                st.executeQuery().use { rs ->
                    rs.next()
                    if (rs.getLong(1) > 0) {
                        error("EMAIL_ALREADY_REGISTERED")
                    }
                }
            }

            val passwordHash = PasswordService.hash(request.password)

            val insertSql = """
                INSERT INTO users (
                    name,
                    email,
                    password_hash,
                    baseline_bpm,
                    baseline_gsr,
                    baseline_movement,
                    is_calibrated,
                    data_retention_consent
                )
                VALUES (?, ?, ?, 70.0, 500.0, 0.100, false, true)
                RETURNING id, name, email
            """.trimIndent()

            connection.prepareStatement(insertSql).use { st ->
                st.setString(1, request.name.trim())
                st.setString(2, email)
                st.setString(3, passwordHash)

                st.executeQuery().use { rs ->
                    rs.next()

                    val userId = UUID.fromString(rs.getString("id"))
                    val name = rs.getString("name")
                    val userEmail = rs.getString("email")

                    val token = JwtService.generateToken(
                        userId = userId,
                        email = userEmail
                    )

                    return AuthResponse(
                        userId = userId.toString(),
                        name = name,
                        email = userEmail,
                        token = token.token,
                        expiresAt = token.expiresAt.toString()
                    )
                }
            }
        }
    }

    fun login(request: LoginRequest): AuthResponse {
        DatabaseFactory.getDataSource().connection.use { connection ->
            val email = request.email.trim().lowercase()

            val sql = """
                SELECT id, name, email, password_hash
                FROM users
                WHERE email = ?
                  AND deleted_at IS NULL
                LIMIT 1
            """.trimIndent()

            connection.prepareStatement(sql).use { st ->
                st.setString(1, email)

                st.executeQuery().use { rs ->
                    if (!rs.next()) {
                        error("INVALID_CREDENTIALS")
                    }

                    val passwordHash = rs.getString("password_hash")

                    if (!PasswordService.verify(request.password, passwordHash)) {
                        error("INVALID_CREDENTIALS")
                    }

                    val userId = UUID.fromString(rs.getString("id"))
                    val name = rs.getString("name")
                    val userEmail = rs.getString("email")

                    val token = JwtService.generateToken(
                        userId = userId,
                        email = userEmail
                    )

                    return AuthResponse(
                        userId = userId.toString(),
                        name = name,
                        email = userEmail,
                        token = token.token,
                        expiresAt = token.expiresAt.toString()
                    )
                }
            }
        }
    }

    fun refresh(userId: UUID, email: String): com.stressless.dto.auth.RefreshTokenResponse {
        val token = JwtService.generateToken(
            userId = userId,
            email = email
        )

        return com.stressless.dto.auth.RefreshTokenResponse(
            token = token.token,
            expiresAt = token.expiresAt.toString()
        )
    }

    private fun validateRegisterRequest(request: RegisterRequest) {
        require(request.name.trim().isNotBlank()) {
            "NAME_REQUIRED"
        }

        require(request.email.contains("@")) {
            "EMAIL_INVALID"
        }

        require(request.password.length >= 8) {
            "PASSWORD_TOO_SHORT"
        }
    }
}