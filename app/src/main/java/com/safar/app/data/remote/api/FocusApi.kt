package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.DeleteEkagraSessionResponse
import com.safar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safar.app.data.remote.dto.EkagraSessionsResponse
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safar.app.data.remote.dto.SaveEkagraSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FocusApi {

    @GET("focus-sessions/stats")
    suspend fun getStats(): Response<FocusStatsResponse>

    @GET("ekagra-sessions")
    suspend fun getSessions(): Response<EkagraSessionsResponse>

    @GET("ekagra-sessions/analytics")
    suspend fun getEkagraAnalytics(): Response<EkagraAnalyticsStatsDto>

    @POST("ekagra-sessions/save")
    suspend fun saveSession(@Body request: SaveEkagraSessionRequest): Response<SaveEkagraSessionResponse>

    @DELETE("ekagra-sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<DeleteEkagraSessionResponse>
}
