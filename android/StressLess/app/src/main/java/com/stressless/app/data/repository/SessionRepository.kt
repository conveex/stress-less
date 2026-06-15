package com.stressless.app.data.repository

import com.stressless.app.data.local.SessionData
import com.stressless.app.data.local.SessionPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionPreferences: SessionPreferences
) {
    val sessionFlow: Flow<SessionData> = sessionPreferences.sessionFlow

    suspend fun clearSession() {
        sessionPreferences.clearSession()
    }
}