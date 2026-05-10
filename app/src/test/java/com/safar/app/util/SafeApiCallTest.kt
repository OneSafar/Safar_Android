package com.safar.app.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SafeApiCallTest {

    @Test
    fun `returns success body`() = runTest {
        val result = safeApiCall { Response.success("ok") }

        assertEquals(Resource.Success("ok"), result)
    }

    @Test
    fun `returns error for empty success body`() = runTest {
        val result = safeApiCall<String> { Response.success(null) }

        assertEquals(Resource.Error<String>("Empty response body"), result)
    }

    @Test
    fun `returns api error message and code`() = runTest {
        val body = """{"message":"Invalid credentials"}"""
            .toResponseBody("application/json".toMediaType())

        val result = safeApiCall<String> { Response.error(401, body) }

        assertEquals(Resource.Error<String>("Invalid credentials", 401), result)
    }

    @Test
    fun `maps unknown host`() = runTest {
        val result = safeApiCall<String> { throw UnknownHostException() }

        assertEquals(
            Resource.Error<String>("Could not reach SAFAR. Please check your internet connection."),
            result,
        )
    }

    @Test
    fun `maps timeout`() = runTest {
        val result = safeApiCall<String> { throw SocketTimeoutException() }

        assertEquals(
            Resource.Error<String>("SAFAR is taking too long to respond. Please try again."),
            result,
        )
    }

    @Test
    fun `maps io error`() = runTest {
        val result = safeApiCall<String> { throw IOException() }

        assertEquals(
            Resource.Error<String>("Could not connect to SAFAR. Please try again."),
            result,
        )
    }

    @Test
    fun `rethrows cancellation`() = runTest {
        val result = runCatching {
            safeApiCall<String> { throw kotlinx.coroutines.CancellationException("cancelled") }
        }

        assertTrue(result.exceptionOrNull() is kotlinx.coroutines.CancellationException)
    }
}
