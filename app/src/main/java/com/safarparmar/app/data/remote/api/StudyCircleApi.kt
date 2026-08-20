package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

import retrofit2.http.Query

interface StudyCircleApi {
    @GET("study-circles")
    suspend fun getMyCircles(): Response<StudyCirclesResponse>

    @GET("study-circles/public")
    suspend fun getPublicCircles(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 15,
        @Query("search") search: String? = null,
    ): Response<PublicStudyCirclesResponse>

    @GET("study-circles/live-summary")
    suspend fun getLiveSummary(): Response<StudyCircleLiveSummaryDto>

    @POST("study-circles")
    suspend fun createCircle(@Body request: CreateStudyCircleRequest): Response<CreatedStudyCircleResponse>

    @POST("study-circles/join")
    suspend fun joinWithCode(@Body request: JoinStudyCircleRequest): Response<JoinStudyCircleResponse>

    @POST("study-circles/{circleId}/join-public")
    suspend fun joinPublic(@Path("circleId") circleId: String): Response<JoinStudyCircleResponse>

    @GET("study-circles/{circleId}")
    suspend fun getCircle(@Path("circleId") circleId: String): Response<StudyCircleResponse>

    @GET("study-circles/{circleId}/leaderboard")
    suspend fun getLeaderboard(@Path("circleId") circleId: String): Response<StudyCircleLeaderboardResponse>

    @PATCH("study-circles/{circleId}/visibility")
    suspend fun setVisibility(
        @Path("circleId") circleId: String,
        @Body request: SetStudyCircleVisibilityRequest,
    ): Response<StudyCircleVisibilityResponse>

    @PATCH("study-circles/{circleId}/name")
    suspend fun updateCircleName(
        @Path("circleId") circleId: String,
        @Body request: UpdateStudyCircleNameRequest,
    ): Response<StudyCircleRenameResponse>

    @POST("study-circles/{circleId}/leave")
    suspend fun leaveCircle(@Path("circleId") circleId: String): Response<StudyCircleActionResponse>

    @DELETE("study-circles/{circleId}")
    suspend fun deleteCircle(@Path("circleId") circleId: String): Response<StudyCircleActionResponse>

    @DELETE("study-circles/{circleId}/members/{userId}")
    suspend fun removeMember(
        @Path("circleId") circleId: String,
        @Path("userId") userId: String,
    ): Response<StudyCircleActionResponse>
}
