package com.safarparmar.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val exam: String? = null,
    val stage: String? = null,
    val gender: String? = null
)

@Immutable
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatar: String? = null,
    val examType: String? = null,
    val preparationStage: String? = null,
    val gender: String? = null,
    val isAdmin: Boolean = false,
)

@Immutable
data class ForgotPasswordResult(
    val message: String,
    val resetToken: String?
)

