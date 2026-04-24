package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.ActivateSessionRequest
import com.safar.app.data.remote.dto.ActivateSessionResponse
import com.safar.app.data.remote.dto.CompleteSessionRequest
import com.safar.app.data.remote.dto.CompleteSessionResponse
import com.safar.app.data.remote.dto.EkagraSessionsResponse
import com.safar.app.data.remote.dto.FocusStatsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FocusApi {

    @GET("focus-sessions/stats")
    suspend fun getStats(): Response<FocusStatsResponse>

    @GET("ekagra-sessions")
    suspend fun getSessions(): Response<EkagraSessionsResponse>

    @GET("ekagra-sessions/active")
    suspend fun getActiveSession(): Response<ActiveSessionResponse>

    @POST("ekagra-sessions/activate")
    suspend fun activateSession(@Body request: ActivateSessionRequest): Response<ActivateSessionResponse>

    @POST("ekagra-sessions/{sessionId}/complete")
    suspend fun completeSession(
        @Path("sessionId") sessionId: String,
        @Body request: CompleteSessionRequest,
    ): Response<CompleteSessionResponse>
}

/** Wrapper for GET ekagra-sessions/active — session is null when none active */
data class ActiveSessionResponse(
    val session: com.safar.app.data.remote.dto.EkagraSession?,
)
