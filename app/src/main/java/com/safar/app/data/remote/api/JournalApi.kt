package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface JournalApi {
    // GET /journal → list
    @GET("journal")
    suspend fun getJournals(): Response<List<JournalDto>>

    // POST /journal
    @POST("journal")
    suspend fun createJournal(@Body request: CreateJournalRequest): Response<JournalDto>
}
