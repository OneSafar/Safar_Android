package com.safarparmar.app.feature.habits.data

import com.google.gson.JsonObject
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class HabitBatchRequest(
    val mutations: List<JsonObject>
)

data class HabitBatchResponse(
    val version: Long,
    val acceptedMutationIds: List<String>
)

data class HabitSnapshotResponse(
    val version: Long,
    val habits: List<HabitRemoteRecord>,
    val cursor: String?,
    val upToDate: Boolean
)

data class HabitRemoteRecord(
    val id: String,
    val name: String,
    val targetDays: List<Int>,
    val isEveryDay: Boolean,
    val isActive: Boolean,
    val scheduledSince: String,
    val order: Int,
    val reminderTime: String?,
    val archivedAt: String?,
    val deleted: Boolean,
    val revisions: List<HabitRemoteRevision>,
    val completions: List<HabitRemoteCompletion>
)

data class HabitRemoteRevision(
    val effectiveFrom: String,
    val targetDays: List<Int>,
    val isEveryDay: Boolean
)

data class HabitRemoteCompletion(
    val date: String,
    val completed: Boolean
)

interface HabitApi {
    @POST("habits/{owner}/mutations")
    suspend fun mutate(
        @Path("owner") owner: String,
        @Body request: HabitBatchRequest
    ): HabitBatchResponse

    @GET("habits/{owner}/snapshot")
    suspend fun snapshot(
        @Path("owner") owner: String,
        @Query("knownVersion") knownVersion: Long?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int
    ): HabitSnapshotResponse
}
