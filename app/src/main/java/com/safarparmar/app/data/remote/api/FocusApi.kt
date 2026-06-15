package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.DeleteEkagraSessionResponse
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safarparmar.app.data.remote.dto.EkagraSessionsResponse
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface FocusApi {

    @GET("ekagra-sessions/stats")
    suspend fun getStats(): Response<FocusStatsResponse>

    @GET("ekagra-sessions")
    suspend fun getSessions(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50,
    ): Response<EkagraSessionsResponse>

    @GET("ekagra-sessions/analytics")
    suspend fun getEkagraAnalytics(): Response<EkagraAnalyticsStatsDto>

    @POST("ekagra-sessions/save")
    suspend fun saveSession(@Body request: SaveEkagraSessionRequest): Response<SaveEkagraSessionResponse>

    @DELETE("ekagra-sessions/{sessionId}")
    suspend fun deleteSession(@Path("sessionId") sessionId: String): Response<DeleteEkagraSessionResponse>
}
