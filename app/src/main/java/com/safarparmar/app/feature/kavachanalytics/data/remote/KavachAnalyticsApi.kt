package com.safarparmar.app.feature.kavachanalytics.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Versioned analytics interface, deliberately separate from the achievement-oriented
 * `achievements/kavach-event` endpoint so gamification and measurement can evolve
 * independently.
 */
interface KavachAnalyticsApi {

    @POST("kavach-analytics/v1/daily-aggregates")
    suspend fun uploadDailyAggregates(
        @Body body: DailyAggregateBatchRequest,
    ): Response<BatchAckResponse>

    @POST("kavach-analytics/v1/sessions")
    suspend fun uploadSessions(
        @Body body: SessionBatchRequest,
    ): Response<BatchAckResponse>

    @POST("kavach-analytics/v1/app-classifications")
    suspend fun uploadAppClassifications(
        @Body body: AppClassificationBatchRequest,
    ): Response<BatchAckResponse>

    @GET("kavach-analytics/v1/app-classifications")
    suspend fun getAppClassifications(): Response<AppClassificationListResponse>

    @GET("kavach-analytics/v1/report")
    suspend fun getReport(
        @Query("start") start: String,
        @Query("end") end: String,
        @Query("timezone") timezone: String,
    ): Response<KavachReportResponse>
}
