package com.stressless.app.data.remote.api

import com.stressless.app.data.remote.dto.app.AppHomeResponseDto
import com.stressless.app.data.remote.dto.app.ChangeOperationalStateRequestDto
import com.stressless.app.data.remote.dto.app.ChangeOperationalStateResponseDto
import com.stressless.app.data.remote.dto.app.ManualHubCommandRequestDto
import com.stressless.app.data.remote.dto.app.ManualHubCommandResponseDto
import com.stressless.app.data.remote.dto.app.ProfilesResponseDto
import com.stressless.app.data.remote.dto.app.RoomPrimaryResponseDto
import com.stressless.app.data.remote.dto.app.StressRecentEventsResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @POST("api/v1/hubs/{hubLogicalId}/operational-state")
    suspend fun changeOperationalState(
        @Path("hubLogicalId") hubLogicalId: String,
        @Body request: ChangeOperationalStateRequestDto
    ): Response<ChangeOperationalStateResponseDto>

    @POST("api/v1/hubs/{hubLogicalId}/commands")
    suspend fun sendManualCommand(
        @Path("hubLogicalId") hubLogicalId: String,
        @Body request: ManualHubCommandRequestDto
    ): Response<ManualHubCommandResponseDto>
}