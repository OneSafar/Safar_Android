package com.safar.app.domain.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val exam: String? = null,
    val stage: String? = null,
    val gender: String? = null
)

data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatar: String? = null,
    val examType: String? = null,
    val preparationStage: String? = null,
    val gender: String? = null
)
