package com.safar.app.util

import android.util.Log
import com.safar.app.BuildConfig
import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONObject
import retrofit2.Response

sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val code: Int? = null) : Resource<T>()
    class Loading<T> : Resource<T>()
}

private const val MAX_RATE_LIMIT_RETRIES = 1
private const val DEFAULT_RATE_LIMIT_BACKOFF_MS = 1_200L
private const val MAX_RATE_LIMIT_BACKOFF_MS = 4_000L

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> {
    var attempt = 0
    while (true) {
        try {
            val response = call()
            if (response.isSuccessful) {
                return response.body()?.let { Resource.Success(it) }
                    ?: Resource.Error("Empty response body")
            }

            val code = response.code()
            // Transient 429s — typically from carrier-grade NAT bursts. Sleep based on
            // the server's Retry-After header (capped) and retry once before failing.
            if (code == 429 && attempt < MAX_RATE_LIMIT_RETRIES) {
                val delayMs = parseRetryAfterMs(response.headers()["Retry-After"])
                if (BuildConfig.DEBUG) {
                    Log.w(
                        TAG_API,
                        "HTTP 429 Too Many Requests on ${response.raw().request.url}; retrying in ${delayMs}ms",
                    )
                }
                attempt += 1
                delay(delayMs)
                continue
            }

            val errorMessage = try {
                val errorBody = response.errorBody()?.string()
                val json = JSONObject(errorBody ?: "")
                json.optString("message")
                    .ifBlank { json.optString("error") }
                    .ifBlank { "Unknown error" }
            } catch (e: Exception) {
                "Unknown error"
            }
            if (code == 429 && BuildConfig.DEBUG) {
                Log.w(
                    TAG_API,
                    "HTTP 429 Too Many Requests (final): ${response.raw().request.url} — $errorMessage",
                )
            }
            return Resource.Error(errorMessage, code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnknownHostException) {
            return Resource.Error("Could not reach SAFAR. Please check your internet connection.")
        } catch (e: SocketTimeoutException) {
            return Resource.Error("SAFAR is taking too long to respond. Please try again.")
        } catch (e: EOFException) {
            return Resource.Error("Connection to SAFAR was interrupted. Please try again.")
        } catch (e: IOException) {
            return Resource.Error("Could not connect to SAFAR. Please try again.")
        } catch (e: Exception) {
            return Resource.Error("Something went wrong. Please try again.")
        }
    }
}

private fun parseRetryAfterMs(retryAfter: String?): Long {
    if (retryAfter.isNullOrBlank()) return DEFAULT_RATE_LIMIT_BACKOFF_MS
    val seconds = retryAfter.trim().toLongOrNull()
    if (seconds != null && seconds > 0) {
        return (seconds * 1000L).coerceAtMost(MAX_RATE_LIMIT_BACKOFF_MS)
    }
    return DEFAULT_RATE_LIMIT_BACKOFF_MS
}

private const val TAG_API = "SafarApi"
