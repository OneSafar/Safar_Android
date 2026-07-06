package com.safarparmar.app.data.repository

import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.local.PersistentCookieStore
import com.safarparmar.app.data.remote.api.AuthApi
import com.safarparmar.app.data.remote.api.NotificationApi
import com.safarparmar.app.data.remote.dto.DeviceTokenRevokeRequest
import com.safarparmar.app.data.remote.dto.*
import com.safarparmar.app.di.IoDispatcher
import com.safarparmar.app.domain.model.User
import com.safarparmar.app.domain.model.UserProfile
import com.safarparmar.app.domain.model.ForgotPasswordResult
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.notifications.NotificationTokenRegistrar
import com.safarparmar.app.util.decodeIsAdminClaim
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val notificationApi: NotificationApi,
    private val dataStore: SafarDataStore,
    private val cookieStore: PersistentCookieStore,
    private val notificationTokenRegistrar: NotificationTokenRegistrar,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = dataStore.isLoggedIn

    override suspend fun login(email: String, password: String): Resource<User> {
        val r = safeApiCall { authApi.login(LoginRequest(email, password)) }
        return when (r) {
            is Resource.Success -> {
                val u = r.data.user
                val token = r.data.accessToken
                if (token.isNullOrBlank()) return Resource.Error("Login response did not include a session token")
                dataStore.setAuthToken(token)
                dataStore.setLoggedIn(true)
                dataStore.setUserId(u?.id)
                dataStore.setUserName(u?.name ?: "")
                dataStore.setUserEmail(u?.email)
                dataStore.setUserAvatar(u?.avatar)
                dataStore.setIsAdmin(u?.isAdmin ?: decodeIsAdminClaim(token))
                // Do not block sign-in on FCM registration — network can be slow after cold start.
                CoroutineScope(SupervisorJob() + ioDispatcher).launch {
                    runCatching { notificationTokenRegistrar.registerStoredTokenIfNeeded(force = true) }
                }
                Resource.Success(User(id = u?.id ?: "", name = u?.name ?: "", email = u?.email ?: "", photoUrl = u?.avatar, exam = u?.examType, stage = u?.preparationStage, gender = u?.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun googleLogin(idToken: String): Resource<User> {
        val r = safeApiCall { authApi.googleLogin(GoogleLoginRequest(token = idToken)) }
        return when (r) {
            is Resource.Success -> {
                val u = r.data.user
                val token = r.data.accessToken
                if (token.isNullOrBlank()) return Resource.Error("Google login response did not include a session token")
                dataStore.setAuthToken(token)
                dataStore.setLoggedIn(true)
                dataStore.setUserId(u?.id)
                dataStore.setUserName(u?.name ?: "")
                dataStore.setUserEmail(u?.email)
                dataStore.setUserAvatar(u?.avatar)
                dataStore.setIsAdmin(u?.isAdmin ?: decodeIsAdminClaim(token))
                // Do not block sign-in on FCM registration — network can be slow after cold start.
                CoroutineScope(SupervisorJob() + ioDispatcher).launch {
                    runCatching { notificationTokenRegistrar.registerStoredTokenIfNeeded(force = true) }
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
                val token = r.data.accessToken
                if (token.isNullOrBlank()) return Resource.Error("Signup response did not include a session token")
                dataStore.setAuthToken(token)
                dataStore.setLoggedIn(true)
                dataStore.setUserId(u?.id)
                dataStore.setUserName(u?.name ?: "")
                dataStore.setUserEmail(u?.email)
                dataStore.setUserAvatar(u?.avatar)
                dataStore.setIsAdmin(u?.isAdmin ?: decodeIsAdminClaim(token))
                // Do not block sign-in on FCM registration — network can be slow after cold start.
                CoroutineScope(SupervisorJob() + ioDispatcher).launch {
                    runCatching { notificationTokenRegistrar.registerStoredTokenIfNeeded(force = true) }
                }
                Resource.Success(User(id = u?.id ?: "", name = u?.name ?: "", email = u?.email ?: "", photoUrl = u?.avatar, exam = u?.examType, stage = u?.preparationStage, gender = u?.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun forgotPassword(email: String): Resource<ForgotPasswordResult> {
        val r = safeApiCall { authApi.forgotPassword(ForgotPasswordRequest(email)) }
        return when (r) {
            is Resource.Success -> Resource.Success(
                ForgotPasswordResult(
                    message = r.data.message ?: "",
                    resetToken = r.data.resetToken
                )
            )
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun resetPasswordConfirm(token: String, newPassword: String): Resource<Unit> {
        val r = safeApiCall { authApi.resetPasswordConfirm(ResetPasswordConfirmRequest(token, newPassword)) }
        return when (r) {
            is Resource.Success -> Resource.Success(Unit)
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun logout(): Resource<Unit> {
        runCatching { safeApiCall { authApi.logout() } }
        val token = dataStore.fcmToken.first()
        if (!token.isNullOrBlank()) {
            runCatching { safeApiCall { notificationApi.revokeDeviceToken(DeviceTokenRevokeRequest(token)) } }
        }
        dataStore.setLoggedIn(false)
        dataStore.clearSession()
        cookieStore.removeAll()
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
                if (u?.email != null) dataStore.setUserEmail(u.email)
                if (u?.avatar != null) dataStore.setUserAvatar(u.avatar)
                dataStore.setIsAdmin(u?.isAdmin == true)
                Resource.Success(
                    UserProfile(
                        id = u?.id ?: "",
                        name = u?.name ?: "",
                        email = u?.email ?: "",
                        avatar = u?.avatar,
                        examType = u?.examType,
                        preparationStage = u?.preparationStage,
                        gender = u?.gender,
                        isAdmin = u?.isAdmin == true,
                    ),
                )
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
                if (r.data.email != null) dataStore.setUserEmail(r.data.email)
                if (r.data.avatar != null) dataStore.setUserAvatar(r.data.avatar)
                Resource.Success(UserProfile(id = r.data.id ?: "", name = r.data.name ?: "", email = r.data.email ?: "", avatar = r.data.avatar, examType = r.data.examType, preparationStage = r.data.preparationStage, gender = r.data.gender))
            }
            is Resource.Error   -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun uploadAvatar(avatar: MultipartBody.Part): Resource<String> {
        val r = safeApiCall { authApi.uploadAvatar(avatar) }
        return when (r) {
            is Resource.Success -> {
                val url = r.data.url
                if (url.isNullOrBlank()) {
                    Resource.Error(r.data.message ?: "Avatar upload failed")
                } else {
                    dataStore.setUserAvatar(url)
                    Resource.Success(url)
                }
            }
            is Resource.Error -> Resource.Error(r.message)
            is Resource.Loading -> Resource.Loading()
        }
    }
}
