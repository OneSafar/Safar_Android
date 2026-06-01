package com.safarparmar.app.data.remote.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface SyllabusApi {
    @Headers("X-Timeout-Seconds: 120")
    @POST("syllabus/structure-preview")
    suspend fun structureSyllabusPreview(
        @Body request: StructureSyllabusRequest,
    ): Response<StructureSyllabusResponse>
}
