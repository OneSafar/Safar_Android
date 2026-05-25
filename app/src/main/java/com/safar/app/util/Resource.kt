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
    /** [code] = HTTP status; [errorCode] = API body `code` (e.g. CONFLICT, TOPIC_LIMIT). */
    data class Error<T>(val message: String, val code: Int? = null, val errorCode: String? = null) : Resource<T>()
    class Loading<T> : Resource<T>()
}

data class ApiErrorBody(
    val message: String? = null,
    val error: String? = null,
    val code: String? = null,
)

fun parseApiErrorBody(raw: String?): ApiErrorBody {
    if (raw.isNullOrBlank()) return ApiErrorBody()
    return try {
        val json = JSONObject(raw)
        ApiErrorBody(
            message = json.optString("message").ifBlank { null }
                ?: json.optString("error").ifBlank { null },
            error = json.optString("error").ifBlank { null },
            code = json.optString("code").ifBlank { null },
        )
    } catch (_: Exception) {
        ApiErrorBody()
    }
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

            val errorBodyRaw = response.errorBody()?.string()
            val parsed = parseApiErrorBody(errorBodyRaw)
            val isHtml = errorBodyRaw?.trimStart()?.startsWith("<") == true
            val errorMessage = parsed.message?.ifBlank { parsed.error }
                ?: parsed.error
                ?: if (isHtml) "Server error HTTP $code (HTML response)" else "Unknown error"
            
            if (errorMessage == "Unknown error" || isHtml) {
                Log.e(TAG_API, "Unknown/HTML error HTTP $code: ${errorBodyRaw?.take(500)}")
            }
            if (code == 429 && BuildConfig.DEBUG) {
                Log.w(
                    TAG_API,
                    "HTTP 429 Too Many Requests (final): ${response.raw().request.url} — $errorMessage",
                )
            }
            return Resource.Error(errorMessage, code, parsed.code)
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
            Log.e(TAG_API, "SafeApiCall caught exception", e)
            return Resource.Error(e.message ?: "Unknown error")
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
