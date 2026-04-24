package com.safar.app.data.repository

import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.api.AuthApi
import com.safar.app.data.remote.dto.*
import com.safar.app.domain.model.User
import com.safar.app.domain.model.UserProfile
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val dataStore: SafarDataStore
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = dataStore.isLoggedIn

    override suspend fun login(email: String, password: String): Resource<User> {
        val r = safeApiCall { authApi.login(LoginRequest(email, password)) }
        return when (r) {
            is Resource.Success -> {
                val u = r.data.user
                if (r.data.accessToken != null) dataStore.setAuthToken(r.data.accessToken)
                if (u != null) {
                    dataStore.setLoggedIn(true)
                    dataStore.setUserId(u.id)
                    dataStore.setUserName(u.name ?: "")
                    dataStore.setUserAvatar(u.avatar)
                }
                Resource.Success(User(id = u?.id ?: "", name = u?.name ?: "", email = u?.email ?: "", photoUrl = u?.avatar, exam = u?.examType, stage = u?.preparationStage, gender = u?.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun register(name: String, email: String, password: String, exam: String?, stage: String?, gender: String?, photoUrl: String?): Resource<User> {
        val r = safeApiCall { authApi.signup(SignupRequest(name, email, password, exam, stage, gender, photoUrl)) }
        return when (r) {
            is Resource.Success -> {
                val u = r.data.user
                if (r.data.accessToken != null) dataStore.setAuthToken(r.data.accessToken)
                if (u != null) { dataStore.setLoggedIn(true); dataStore.setUserId(u.id); dataStore.setUserName(u.name ?: ""); dataStore.setUserAvatar(u.avatar) }
                Resource.Success(User(id = u?.id ?: "", name = u?.name ?: "", email = u?.email ?: "", photoUrl = u?.avatar, exam = u?.examType, stage = u?.preparationStage, gender = u?.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun forgotPassword(email: String): Resource<String> {
        val r = safeApiCall { authApi.forgotPassword(ForgotPasswordRequest(email)) }
        return when (r) {
            is Resource.Success -> Resource.Success(r.data.message ?: "")
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun logout(): Resource<Unit> {
        runCatching { safeApiCall { authApi.logout() } }
        dataStore.setLoggedIn(false)
        dataStore.setAuthToken("")
        return Resource.Success(Unit)
    }

    override suspend fun refreshToken(): Resource<Unit> = Resource.Success(Unit)

    override suspend fun getMe(): Resource<UserProfile> {
        val r = safeApiCall { authApi.getMe() }
        return when (r) {
            is Resource.Success -> {
                val u = r.data.user
                if (u?.id != null) dataStore.setUserId(u.id)
                if (u?.name != null) dataStore.setUserName(u.name)
                if (u?.avatar != null) dataStore.setUserAvatar(u.avatar)
                Resource.Success(UserProfile(id = u?.id ?: "", name = u?.name ?: "", email = u?.email ?: "", avatar = u?.avatar, examType = u?.examType, preparationStage = u?.preparationStage, gender = u?.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun updateProfile(name: String?, examType: String?, preparationStage: String?, gender: String?, avatar: String?): Resource<UserProfile> {
        val r = safeApiCall { authApi.updateProfile(UpdateProfileRequest(name = name, examType = examType, preparationStage = preparationStage, gender = gender, avatar = avatar)) }
        return when (r) {
            is Resource.Success -> {
                if (r.data.name != null) dataStore.setUserName(r.data.name)
                if (r.data.avatar != null) dataStore.setUserAvatar(r.data.avatar)
                Resource.Success(UserProfile(id = r.data.id ?: "", name = r.data.name ?: "", email = r.data.email ?: "", avatar = r.data.avatar, examType = r.data.examType, preparationStage = r.data.preparationStage, gender = r.data.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }
}
