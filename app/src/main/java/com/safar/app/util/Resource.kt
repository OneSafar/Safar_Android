package com.safar.app.util

import retrofit2.Response

sealed class Resource<T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error<T>(val message: String, val code: Int? = null) : Resource<T>()
    class Loading<T> : Resource<T>()
}

suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Resource<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            response.body()?.let {
                Resource.Success(it)
            } ?: Resource.Error("Empty response body")
        } else {
            Resource.Error(response.message() ?: "Unknown error", response.code())
        }
    } catch (e: Exception) {
        Resource.Error(e.localizedMessage ?: "Network error")
    }
}
