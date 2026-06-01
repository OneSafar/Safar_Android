package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface NishthaApi {
    // GET /moods
    @GET("moods")
    suspend fun getMoods(): Response<List<MoodDto>>

    // POST /moods → 201 Created
    @POST("moods")
    suspend fun createMood(@Body request: CreateMoodRequest): Response<MoodDto>
}
