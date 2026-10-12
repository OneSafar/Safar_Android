package com.safarparmar.app.feature.habits

import com.google.gson.JsonObject
import com.safarparmar.app.feature.habits.data.*
import com.safarparmar.app.feature.habits.di.HabitModule
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class HabitApiContractTest {
    @Test fun `Android uses the website endpoints owner header null fields and acknowledgement contract`() = runTest {
        var requests = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("alice", request.header("X-Habit-Owner"))
            val payload = if (request.method == "POST") {
                assertEquals("/api/habits/sync/mutations", request.url.encodedPath)
                val buffer = Buffer()
                request.body!!.writeTo(buffer)
                val body = buffer.readUtf8()
                assertTrue("Explicit null reminder must reach the backend", body.contains("\"reminderTime\":null"))
                """{"version":1,"acknowledged":["mutation"]}"""
            } else {
                assertEquals("/api/habits/sync", request.url.encodedPath)
                assertEquals("1", request.url.queryParameter("knownVersion"))
                """{"version":1,"habits":[],"nextCursor":null,"unchanged":true}"""
            }
            requests++
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body(payload.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val retrofit = Retrofit.Builder().baseUrl("https://example.test/api/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val api = HabitModule.provideHabitApi(retrofit)
        val mutation = JsonObject().apply {
            addProperty("mutationId", "mutation")
            addProperty("habitId", "habit")
            addProperty("type", "create")
            add("reminderTime", com.google.gson.JsonNull.INSTANCE)
        }
        assertEquals(listOf("mutation"), api.mutate("alice", HabitBatchRequest(listOf(mutation))).acknowledged)
        assertTrue(api.snapshot("alice", 1).unchanged)
        assertEquals(2, requests)
    }
}
