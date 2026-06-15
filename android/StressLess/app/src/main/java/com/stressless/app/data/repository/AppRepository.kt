package com.stressless.app.data.repository

import com.stressless.app.data.remote.api.AppApi
import com.stressless.app.data.remote.dto.app.AppHomeResponseDto
import com.stressless.app.data.remote.dto.app.ProfilesResponseDto
import com.stressless.app.data.remote.dto.app.RoomPrimaryResponseDto
import com.stressless.app.data.remote.dto.app.StressRecentEventsResponseDto
import com.stressless.app.util.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val appApi: AppApi
) {

    suspend fun getHome(): ApiResult<AppHomeResponseDto> {
        return safeCall {
            appApi.getHome()
        }
    }

    suspend fun getPrimaryRoom(): ApiResult<RoomPrimaryResponseDto> {
        return safeCall {
            appApi.getPrimaryRoom()
        }
    }

    suspend fun getProfiles(): ApiResult<ProfilesResponseDto> {
        return safeCall {
            appApi.getProfiles()
        }
    }

    suspend fun getRecentEvents(limit: Int = 20): ApiResult<StressRecentEventsResponseDto> {
        return safeCall {
            appApi.getRecentEvents(limit)
        }
    }

    private suspend fun <T> safeCall(
        call: suspend () -> retrofit2.Response<T>
    ): ApiResult<T> {
        return try {
            val response = call()

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    ApiResult.Success(body)
                } else {
                    ApiResult.Error("Respuesta vacía del servidor.")
                }
            } else {
                ApiResult.Error(mapHttpError(response.code()))
            }
        } catch (ex: Exception) {
            ApiResult.Error(
                ex.message ?: "No se pudo conectar con el servidor."
            )
        }
    }

    private fun mapHttpError(code: Int): String {
        return when (code) {
            401 -> "Tu sesión expiró. Vuelve a iniciar sesión."
            403 -> "No tienes permisos para realizar esta acción."
            404 -> "No se encontró la información solicitada."
            500 -> "Error interno del servidor."
            else -> "Error inesperado. Código: $code"
        }
    }
}