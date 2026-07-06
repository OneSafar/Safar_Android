package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/signup") suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>
    @POST("auth/login") suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    @POST("auth/google") suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<AuthResponse>
    @POST("auth/logout") suspend fun logout(): Response<MessageResponse>
    @POST("auth/forgot-password") suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>
    @POST("auth/reset-password/confirm") suspend fun resetPasswordConfirm(@Body request: ResetPasswordConfirmRequest): Response<MessageResponse>
    @GET("auth/me") suspend fun getMe(): Response<MeResponse>
    @PATCH("auth/profile") suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<UserDto>
    @Multipart
    @POST("upload/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<UploadAvatarResponse>
    @GET("auth/login-history")
    suspend fun getLoginHistory(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30,
    ): Response<List<LoginHistoryItemDto>>
}
