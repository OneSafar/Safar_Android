package com.safar.app.data.repository

import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.AuthApi
import com.safar.app.data.remote.dto.AuthResponse
import com.safar.app.data.remote.dto.ForgotPasswordRequest
import com.safar.app.data.remote.dto.LoginRequest
import com.safar.app.data.remote.dto.SignupRequest
import com.safar.app.domain.model.User
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import java.net.CookieManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: SafarDataStore,
    private val cookieManager: CookieManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Resource<User> {
        val result = safeApiCall { authApi.login(LoginRequest(email, password)) }
        return when (result) {
            is Resource.Success -> {
                dataStore.setUserId(result.data.id)
                dataStore.setLoggedIn(true)
                Resource.Success(result.data.toDomain())
            }
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        password: String,
        exam: String?,
        stage: String?,
        gender: String?,
        photoUrl: String?
    ): Resource<User> {
        val result = safeApiCall {
            authApi.signup(
                SignupRequest(
                    name             = name,
                    email            = email,
                    password         = password,
                    examType         = exam,
                    preparationStage = stage,
                    gender           = gender,
                    profileImage     = photoUrl
                )
            )
        }
        return when (result) {
            is Resource.Success -> {
                dataStore.setUserId(result.data.id)
                dataStore.setLoggedIn(true)
                Resource.Success(result.data.toDomain())
            }
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        val result = safeApiCall { authApi.forgotPassword(ForgotPasswordRequest(email)) }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.message)
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun logout(): Resource<Unit> {
        safeApiCall { authApi.logout() }
        dataStore.clearSession()
        cookieManager.cookieStore.removeAll()
        return Resource.Success(Unit)
    }

    override suspend fun refreshToken(): Resource<Unit> = Resource.Success(Unit)

    override val isLoggedIn: Flow<Boolean>
        get() = dataStore.isLoggedIn

    private fun AuthResponse.toDomain() = User(
        id       = id       ?: "",
        name     = name     ?: "",
        email    = email    ?: "",
        photoUrl = avatar,
        exam     = examType,
        stage    = preparationStage,
        gender   = gender
    )
}