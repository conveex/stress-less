package com.stressless.security

import org.mindrot.jbcrypt.BCrypt

object PasswordService {

    fun hash(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt(12))
    }

    fun verify(password: String, passwordHash: String): Boolean {
        return try {
            BCrypt.checkpw(password, passwordHash)
        } catch (ex: Exception) {
            false
        }
    }
}