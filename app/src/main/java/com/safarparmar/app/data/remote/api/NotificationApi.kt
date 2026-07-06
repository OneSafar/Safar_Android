package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.AdminBroadcastRequest
import com.safarparmar.app.data.remote.dto.AdminBroadcastResponse
import com.safarparmar.app.data.remote.dto.DeviceTokenRequest
import com.safarparmar.app.data.remote.dto.DeviceTokenRevokeRequest
import com.safarparmar.app.data.remote.dto.MessageResponse
import com.safarparmar.app.data.remote.dto.NotificationHistoryResponse
import com.safarparmar.app.data.remote.dto.NotificationPreferencesRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationApi {
    @POST("device-tokens")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<MessageResponse>

    @PATCH("device-tokens/revoke")
    suspend fun revokeDeviceToken(@Body request: DeviceTokenRevokeRequest): Response<MessageResponse>

    @PATCH("notification-preferences")
    suspend fun updateNotificationPreferences(@Body request: NotificationPreferencesRequest): Response<MessageResponse>

    @POST("admin/notifications/broadcast")
    suspend fun sendAdminBroadcast(@Body request: AdminBroadcastRequest): Response<AdminBroadcastResponse>

    /** Sends only to the signed-in user — does not require admin (for testing composer content). */
    @POST("notifications/test")
    suspend fun sendTestNotification(@Body request: AdminBroadcastRequest): Response<AdminBroadcastResponse>

    @GET("notifications/history")
    suspend fun getNotificationHistory(@Query("page") page: Int = 1, @Query("limit") limit: Int = 20): Response<NotificationHistoryResponse>
}
