package com.safarparmar.app.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class TitliReplyDto(val message: String, @SerializedName("created_at") val createdAt: String = "")
data class TitliQueryDto(val id: String, val message: String,
    @SerializedName(value = "created_at", alternate = ["createdAt"]) val createdAt: String = "",
    val reply: TitliReplyDto? = null)
data class TitliQueriesResponse(val queries: List<TitliQueryDto> = emptyList())
data class TitliSubmitResponse(val feedback: TitliQueryDto)
data class TitliSubmitRequest(val message: String, val requestId: String,
    val type: String = "technical_query", val feature: String = "titli", val page: String = "android/titli", val trigger: String = "help_menu")
interface TitliApi {
    @GET("feedback/titli/mine") suspend fun mine(): TitliQueriesResponse
    @POST("feedback") suspend fun submit(@Body request: TitliSubmitRequest): TitliSubmitResponse
}
