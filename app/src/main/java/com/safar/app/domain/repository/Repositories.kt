package com.safar.app.domain.repository

import com.safar.app.domain.model.*
import com.safar.app.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(name: String, email: String, password: String, exam: String?, stage: String?, gender: String?, photoUrl: String?): Resource<User>
    suspend fun forgotPassword(email: String): Resource<String>
    suspend fun logout(): Resource<Unit>
    suspend fun refreshToken(): Resource<Unit>
    val isLoggedIn: Flow<Boolean>
}
