package com.safar.app.data.repository

import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.AuthApi
import com.safar.app.domain.model.User
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: SafarDataStore
) : AuthRepository {

    override suspend fun login(
        email: String,
        password: String
    ): Resource<User> {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        TODO("Not yet implemented")
    }

    override suspend fun logout(): Resource<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshToken(): Resource<Unit> {
        TODO("Not yet implemented")
    }

    override val isLoggedIn: Flow<Boolean>
        get() = TODO("Not yet implemented")

}
