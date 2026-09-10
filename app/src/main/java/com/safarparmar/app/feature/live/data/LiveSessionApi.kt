package com.safarparmar.app.feature.live.data

import com.safarparmar.app.feature.live.model.LiveSessionResponseDto
import com.safarparmar.app.feature.live.model.LiveSessionsResponseDto
import com.safarparmar.app.feature.live.model.LiveCommunityResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LiveSessionApi {
    @GET("live-sessions/community")
    suspend fun getCommunity(): Response<LiveCommunityResponseDto>

    @GET("live-sessions")
    suspend fun listLiveSessions(
        @Query("courseId") courseId: String? = null,
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<LiveSessionsResponseDto>

    @GET("live-sessions/{id}")
    suspend fun getLiveSession(
        @Path("id") id: String,
    ): Response<LiveSessionResponseDto>
}
