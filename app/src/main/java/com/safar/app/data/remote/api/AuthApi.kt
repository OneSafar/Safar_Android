package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/signup") suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>
    @POST("auth/login") suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    @POST("auth/logout") suspend fun logout(): Response<MessageResponse>
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<MessageResponse>
    @POST("auth/reset-password/confirm") suspend fun resetPasswordConfirm(@Body request: ResetPasswordConfirmRequest): Response<MessageResponse>
    @GET("auth/me") suspend fun getMe(): Response<MeResponse>
    @PATCH("auth/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>
    @GET("auth/login-history") suspend fun getLoginHistory(): Response<List<LoginHistoryItemDto>>
}
