package com.safarparmar.app.domain.repository

import com.safarparmar.app.domain.model.User
import com.safarparmar.app.domain.model.UserProfile
import com.safarparmar.app.util.Resource
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

interface AuthRepository {
    val isLoggedIn: Flow<Boolean>
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(name: String, email: String, password: String, exam: String?, stage: String?, gender: String?, photoUrl: String?): Resource<User>
    suspend fun forgotPassword(email: String): Resource<String>
    suspend fun logout(): Resource<Unit>
    suspend fun refreshToken(): Resource<Unit>
    suspend fun getMe(): Resource<UserProfile>
    suspend fun updateProfile(name: String?, examType: String?, preparationStage: String?, gender: String?, avatar: String?): Resource<UserProfile>
    suspend fun uploadAvatar(avatar: MultipartBody.Part): Resource<String>
}
