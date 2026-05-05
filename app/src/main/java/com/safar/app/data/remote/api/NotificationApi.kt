package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.DeviceTokenRequest
import com.safar.app.data.remote.dto.DeviceTokenRevokeRequest
import com.safar.app.data.remote.dto.MessageResponse
import com.safar.app.data.remote.dto.NotificationPreferencesRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.PATCH
import retrofit2.http.POST

interface NotificationApi {
    @POST("device-tokens")
    suspend fun registerDeviceToken(@Body request: DeviceTokenRequest): Response<MessageResponse>

    @PATCH("device-tokens/revoke")
    suspend fun revokeDeviceToken(@Body request: DeviceTokenRevokeRequest): Response<MessageResponse>

    @PATCH("notification-preferences")
    suspend fun updateNotificationPreferences(@Body request: NotificationPreferencesRequest): Response<MessageResponse>
}
