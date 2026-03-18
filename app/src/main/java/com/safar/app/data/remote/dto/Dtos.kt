package com.safar.app.data.remote.dto

data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val examType: String?,
    val preparationStage: String?,
    val gender: String?,
    val profileImage: String?
)

data class ResetPasswordConfirmRequest(
    val token: String,
    val newPassword: String
)

data class MeResponse(
    val user: UserDto,
    val streaks: StreaksDto
)

data class StreaksDto(
    val login: StreakDto,
    val checkIn: StreakDto,
    val goalCompletion: StreakDto
)


data class StreakDto(
    val type: String,
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActivityDate: Long?,
    val totalCount: Int
)


data class UserDto(
    val id: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val exam: String?,
    val stage: String?,
    val gender: String?,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val isFollowing: Boolean = false
)


data class LoginHistoryResponse(
    val history: List<Long>
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val user: UserDto
)

data class MessageResponse(val message: String, val success: Boolean = true)
data class LoginRequest(val email: String, val password: String)

data class ForgotPasswordRequest(val email: String)
data class UpdateProfileRequest(val name: String?, val photoUrl: String?, val exam: String?, val stage: String?)
