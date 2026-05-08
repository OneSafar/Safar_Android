package com.safar.app.util

import java.io.EOFException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.json.JSONObject
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
            val errorMessage = try {
                val errorBody = response.errorBody()?.string()
                val json = JSONObject(errorBody ?: "")
                json.optString("message")
                    .ifBlank { json.optString("error") }
                    .ifBlank { "Unknown error" }
            } catch (e: Exception) {
                "Unknown error"
            }
            Resource.Error(errorMessage, response.code())
        }
    } catch (e: UnknownHostException) {
        Resource.Error("Could not reach SAFAR. Please check your internet connection.")
    } catch (e: SocketTimeoutException) {
        Resource.Error("SAFAR is taking too long to respond. Please try again.")
    } catch (e: EOFException) {
        Resource.Error("Connection to SAFAR was interrupted. Please try again.")
    } catch (e: IOException) {
        Resource.Error("Could not connect to SAFAR. Please try again.")
    } catch (e: Exception) {
        Resource.Error("Something went wrong. Please try again.")
    }
}
