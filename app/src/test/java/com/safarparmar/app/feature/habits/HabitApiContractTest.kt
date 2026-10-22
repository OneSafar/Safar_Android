package com.safarparmar.app.feature.habits

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
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
    @Test fun `batch wire names remain explicit when fallback field names change`() {
        // Exercise the production DTOs without relying on their Kotlin field names surviving R8.
        val gson = GsonBuilder().setFieldNamingStrategy { field -> "renamed_${field.name}" }.create()
        val mutation = JsonObject().apply { addProperty("mutationId", "mutation") }
        val request = gson.toJsonTree(HabitBatchRequest(listOf(mutation))).asJsonObject
        assertEquals(setOf("mutations"), request.keySet())
        assertEquals(mutation, request.getAsJsonArray("mutations").single())

        val response = gson.fromJson(
            """{"version":42,"acknowledged":["mutation"]}""",
            HabitBatchResponse::class.java
        )
        assertEquals(42L, response.version)
        assertEquals(listOf("mutation"), response.acknowledged)
    }

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
                val envelope = JsonParser.parseString(body).asJsonObject
                assertEquals(setOf("mutations"), envelope.keySet())
                val mutation = envelope.getAsJsonArray("mutations").single().asJsonObject
                assertEquals("mutation", mutation.get("mutationId").asString)
                assertEquals("create", mutation.get("type").asString)
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
        val response = api.mutate("alice", HabitBatchRequest(listOf(mutation)))
        assertEquals(1L, response.version)
        assertEquals(listOf("mutation"), response.acknowledged)
        assertTrue(api.snapshot("alice", 1).unchanged)
        assertEquals(2, requests)
    }
}
