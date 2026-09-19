package com.safarparmar.app.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

data class SupportAvailabilityDto(val enabled: Boolean = false, @SerializedName("staff_access") val staffAccess: Boolean = false)
data class SupportTicketDto(
    val id: String,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("problem_text") val problemText: String,
    @SerializedName("urgency_selfreport") val urgency: String,
    @SerializedName("callback_phone") val callbackPhone: String,
    @SerializedName("contact_preference") val contactPreference: String,
    @SerializedName("is_repeat_contact") val isRepeatContact: Boolean,
    @SerializedName("escalated_by_student") val escalated: Boolean = false,
    val feedback: SupportFeedbackDto? = null,
)
data class SupportFeedbackDto(val rating: Int, val comment: String? = null)
data class SupportTicketResponse(val ticket: SupportTicketDto?, val history: List<SupportTicketDto> = emptyList())
data class CreateSupportTicketRequest(
    @SerializedName("problem_text") val problemText: String,
    @SerializedName("urgency_selfreport") val urgency: String,
    @SerializedName("callback_phone") val callbackPhone: String,
    @SerializedName("contact_preference") val contactPreference: String,
    @SerializedName("is_repeat_contact") val isRepeatContact: Boolean,
)
data class SupportFeedbackRequest(val rating: Int, val comment: String)

interface SupportApi {
    @GET("support/availability") suspend fun availability(): Response<SupportAvailabilityDto>
    @GET("support/tickets/mine") suspend fun mine(): Response<SupportTicketResponse>
    @POST("support/tickets") suspend fun create(@Body request: CreateSupportTicketRequest): Response<SupportTicketResponse>
    @POST("support/tickets/{id}/escalate") suspend fun escalate(@Path("id") id: String): Response<SupportTicketResponse>
    @POST("support/tickets/{id}/feedback") suspend fun feedback(@Path("id") id: String, @Body request: SupportFeedbackRequest): Response<SupportTicketResponse>
}
