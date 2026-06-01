package com.safarparmar.app.feature.live.data

import com.safarparmar.app.feature.live.model.LiveSessionResponseDto
import com.safarparmar.app.feature.live.model.LiveSessionsResponseDto
import com.safarparmar.app.feature.live.model.EndLiveSessionRequest
import com.safarparmar.app.feature.live.model.StartLiveSessionRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface LiveSessionApi {
    @GET("live-sessions")
    suspend fun listLiveSessions(
        @Query("courseId") courseId: String? = null,
        @Query("status") status: String? = null,
    ): Response<LiveSessionsResponseDto>

    @GET("live-sessions/{id}")
    suspend fun getLiveSession(
        @Path("id") id: String,
    ): Response<LiveSessionResponseDto>

    @PATCH("live-sessions/{id}/start")
    suspend fun startLiveSession(
        @Path("id") id: String,
        @Body request: StartLiveSessionRequest,
    ): Response<LiveSessionResponseDto>

    @PATCH("live-sessions/{id}/end")
    suspend fun endLiveSession(
        @Path("id") id: String,
        @Body request: EndLiveSessionRequest = EndLiveSessionRequest(),
    ): Response<LiveSessionResponseDto>
}
