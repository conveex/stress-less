package com.stressless.app.data.remote.api

import com.stressless.app.data.remote.dto.app.AppHomeResponseDto
import com.stressless.app.data.remote.dto.app.ProfilesResponseDto
import com.stressless.app.data.remote.dto.app.RoomPrimaryResponseDto
import com.stressless.app.data.remote.dto.app.StressRecentEventsResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AppApi {

    @GET("api/v1/app/home")
    suspend fun getHome(): Response<AppHomeResponseDto>

    @GET("api/v1/rooms/primary")
    suspend fun getPrimaryRoom(): Response<RoomPrimaryResponseDto>

    @GET("api/v1/profiles")
    suspend fun getProfiles(): Response<ProfilesResponseDto>

    @GET("api/v1/stress/recent-events")
    suspend fun getRecentEvents(
        @Query("limit") limit: Int = 20
    ): Response<StressRecentEventsResponseDto>
}