package com.safar.app.util

import com.safar.app.data.local.SafarDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
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
        val response = chain.proceed(request)
        if (response.code == 401) {
            runBlocking { dataStore.clearSession() }
        }
        return response
    }
}
