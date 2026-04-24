package com.safar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SignupRequest(val name: String, val email: String, val password: String, val examType: String?, val preparationStage: String?, val gender: String?, val profileImage: String?)
data class LoginRequest(val email: String, val password: String)
data class ForgotPasswordRequest(val email: String)
data class ResetPasswordConfirmRequest(val token: String, val newPassword: String)

data class UpdateProfileRequest(
    val name: String? = null,
    val examType: String? = null,
    val preparationStage: String? = null,
    val gender: String? = null,
    val avatar: String? = null
)

data class AuthResponse(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("user")        val user: UserResponse?
)

data class UserResponse(
    @SerializedName("id")               val id: String?,
    @SerializedName("name")             val name: String?,
    @SerializedName("email")            val email: String?,
    @SerializedName("avatar")           val avatar: String?,
    @SerializedName("examType")         val examType: String?,
    @SerializedName("preparationStage") val preparationStage: String?,
    @SerializedName("gender")           val gender: String?
)

data class UserDto(
    val id: String?,
    val name: String?,
    val email: String?,
    val avatar: String?,
    val examType: String?,
    val preparationStage: String?,
    val gender: String?
)

data class MeResponse(val user: UserDto?, val streaks: StreaksDto?)
data class MessageResponse(val message: String?, val success: Boolean = true)
data class LoginHistoryItemDto(val timestamp: String? = null)
