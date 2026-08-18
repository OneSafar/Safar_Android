package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.DeleteEkagraSessionResponse
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safarparmar.app.data.remote.dto.EkagraSessionsResponse
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.data.remote.dto.FocusPresenceRequest
import com.safarparmar.app.data.remote.dto.LinkedEkagraSessionsResponse
import com.safarparmar.app.data.remote.dto.LinkEkagraGoalRequest
import com.safarparmar.app.data.remote.dto.LinkEkagraGoalResponse
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionResponse
import com.safarparmar.app.data.remote.dto.TopicLinkedSessionsResponse
import com.safarparmar.app.data.remote.dto.UpdateEkagraSessionRequest
import com.safarparmar.app.data.remote.dto.UpdateEkagraSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FocusApi {

    @POST("ekagra-sessions/presence")
    suspend fun setFocusPresence(@Body request: FocusPresenceRequest): Response<Unit>

    @GET("ekagra-sessions/stats")
    suspend fun getStats(): Response<FocusStatsResponse>

    @GET("ekagra-sessions")
    suspend fun getSessions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<EkagraSessionsResponse>

    @GET("ekagra-sessions/analytics")
    suspend fun getEkagraAnalytics(): Response<EkagraAnalyticsStatsDto>

    @GET("ekagra-sessions/linked-sessions")
    suspend fun getLinkedSessions(): Response<LinkedEkagraSessionsResponse>

    @GET("ekagra-sessions/topic-linked-sessions")
    suspend fun getTopicLinkedSessions(@Query("planId") planId: String? = null): Response<TopicLinkedSessionsResponse>

    @POST("ekagra-sessions/save")
    suspend fun saveSession(@Body request: SaveEkagraSessionRequest): Response<SaveEkagraSessionResponse>

    @DELETE("ekagra-sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<DeleteEkagraSessionResponse>

    @PATCH("ekagra-sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateEkagraSessionRequest,
    ): Response<UpdateEkagraSessionResponse>

    @POST("ekagra-sessions/{sessionId}/link-goal")
    suspend fun linkSessionToGoal(
        @Path("sessionId") sessionId: String,
        @Body request: LinkEkagraGoalRequest,
    ): Response<LinkEkagraGoalResponse>
}
