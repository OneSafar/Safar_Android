package com.safar.app.util

import com.safar.app.BuildConfig
import com.safar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val dataStore: SafarDataStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token    = runBlocking { dataStore.authToken.first() }
        val language = runBlocking { dataStore.language.first() }
        val request  = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
            addHeader("Accept-Language", language)
        }.build()
        var response = chain.proceed(request)
        if (response.code == 401 && !request.url.encodedPath.contains("/auth/refresh")) {
            response.close()
            val refreshRequest = request.newBuilder()
                .url(BuildConfig.BASE_URL.trimEnd('/') + "/auth/refresh")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val refreshResponse = chain.proceed(refreshRequest)
            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string().orEmpty()
                val refreshedToken = runCatching { JSONObject(body).optString("accessToken").takeIf { it.isNotBlank() } }.getOrNull()
                refreshResponse.close()
                if (refreshedToken != null) {
                    runBlocking { dataStore.setAuthToken(refreshedToken) }
                    response = chain.proceed(request.newBuilder().header("Authorization", "Bearer $refreshedToken").build())
                } else {
                    runBlocking { dataStore.clearSession() }
                }
            } else {
                refreshResponse.close()
                runBlocking { dataStore.clearSession() }
            }
        }
        return response
    }
}
