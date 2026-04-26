package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.ActivateSessionRequest
import com.safar.app.data.remote.dto.ActivateSessionResponse
import com.safar.app.data.remote.dto.CompleteSessionRequest
import com.safar.app.data.remote.dto.CompleteSessionResponse
import com.safar.app.data.remote.dto.DeleteEkagraSessionResponse
import com.safar.app.data.remote.dto.DiscardEkagraSessionResponse
import com.safar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safar.app.data.remote.dto.EkagraSessionsResponse
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.data.remote.dto.UpdateEkagraSessionRequest
import com.safar.app.data.remote.dto.UpdateEkagraSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface FocusApi {

    @GET("focus-sessions/stats")
    suspend fun getStats(): Response<FocusStatsResponse>

    @GET("ekagra-sessions")
    suspend fun getSessions(): Response<EkagraSessionsResponse>

    @GET("ekagra-sessions/active")
    suspend fun getActiveSession(): Response<ActiveSessionResponse>

    @GET("ekagra-sessions/analytics")
    suspend fun getEkagraAnalytics(): Response<EkagraAnalyticsStatsDto>

    @POST("ekagra-sessions/activate")
    suspend fun activateSession(@Body request: ActivateSessionRequest): Response<ActivateSessionResponse>

    @PATCH("ekagra-sessions/{sessionId}")
    suspend fun updateSession(
        @Path("sessionId") sessionId: String,
        @Body request: UpdateEkagraSessionRequest,
    ): Response<UpdateEkagraSessionResponse>

    @POST("ekagra-sessions/{sessionId}/complete")
    suspend fun completeSession(
        @Path("sessionId") sessionId: String,
        @Body request: CompleteSessionRequest,
    ): Response<CompleteSessionResponse>

    @POST("ekagra-sessions/{sessionId}/discard")
    suspend fun discardSession(@Path("sessionId") sessionId: String): Response<DiscardEkagraSessionResponse>

    @DELETE("ekagra-sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<DeleteEkagraSessionResponse>
}

/** Wrapper for GET ekagra-sessions/active — session is null when none active */
data class ActiveSessionResponse(
    val session: com.safar.app.data.remote.dto.EkagraSession?,
)
