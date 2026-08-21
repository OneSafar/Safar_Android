package com.safarparmar.app.util

import com.safarparmar.app.BuildConfig
import com.safarparmar.app.data.local.PersistentCookieStore
import com.safarparmar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val dataStore: SafarDataStore,
    private val cookieStore: PersistentCookieStore,
) : Interceptor {
    private val refreshLock = Any()

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { dataStore.authToken.first() }
        val request  = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
            addHeader("Accept-Language", "en")
        }.build()
        var response = chain.proceed(request)
        if (response.code == 401 && shouldAttemptRefresh(request.url.encodedPath)) {
            response = recoverFromUnauthorized(chain, request, response, token)
        }
        return response
    }

    private fun recoverFromUnauthorized(
        chain: Interceptor.Chain,
        request: okhttp3.Request,
        unauthorizedResponse: Response,
        originalToken: String?,
    ): Response {
        val originalContentType = unauthorizedResponse.body?.contentType()
        val originalBody = unauthorizedResponse.body?.string().orEmpty()
        val originalResponse = unauthorizedResponse.newBuilder()
            .body(originalBody.toResponseBody(originalContentType))
            .build()
        unauthorizedResponse.close()

        val alreadyRefreshedToken = runBlocking { dataStore.authToken.first() }
            ?.takeIf { it.isNotBlank() && it != originalToken }
        if (alreadyRefreshedToken != null) {
            originalResponse.close()
            return chain.proceed(request.withBearer(alreadyRefreshedToken))
        }

        synchronized(refreshLock) {
            val latestToken = runBlocking { dataStore.authToken.first() }
                ?.takeIf { it.isNotBlank() && it != originalToken }
            if (latestToken != null) {
                originalResponse.close()
                return chain.proceed(request.withBearer(latestToken))
            }

            val refreshRequest = request.newBuilder()
                .url(BuildConfig.BASE_URL.trimEnd('/') + "/auth/refresh")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .removeHeader("Authorization")
                .build()
            val refreshResponse = chain.proceed(refreshRequest)
            val refreshBody = refreshResponse.body?.string().orEmpty()
            if (refreshResponse.isSuccessful) {
                val refreshedToken = runCatching {
                    val json = JSONObject(refreshBody)
                    json.optString("accessToken").takeIf { it.isNotBlank() }
                        ?: json.optString("token").takeIf { it.isNotBlank() }
                        ?: json.optJSONObject("data")?.optString("accessToken")?.takeIf { it.isNotBlank() }
                }.getOrNull()
                refreshResponse.close()
                if (refreshedToken != null) {
                    runBlocking { dataStore.setAuthToken(refreshedToken) }
                    originalResponse.close()
                    return chain.proceed(request.withBearer(refreshedToken))
                }
                return originalResponse
            }

            val refreshError = runCatching { JSONObject(refreshBody).optString("error") }.getOrNull()
            val isDefinitiveAuthFailure = refreshResponse.code == 401 ||
                refreshResponse.code == 409 ||
                refreshError in setOf("no_refresh_token", "refresh_token_invalid", "refresh_token_stale")
            refreshResponse.close()
            if (isDefinitiveAuthFailure) {
                runBlocking { dataStore.clearSession() }
                cookieStore.removeAll()
            }
            return originalResponse
        }
    }

    private fun okhttp3.Request.withBearer(token: String): okhttp3.Request =
        newBuilder().header("Authorization", "Bearer $token").build()

    private fun shouldAttemptRefresh(path: String): Boolean {
        val authPathsWithoutSession = listOf(
            "/auth/login",
            "/auth/signup",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/auth/refresh"
        )
        return authPathsWithoutSession.none { path.contains(it) }
    }
}
