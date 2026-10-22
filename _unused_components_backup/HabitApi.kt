package com.safarparmar.app.feature.habits.data

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

data class HabitRemoteRevision(
    @SerializedName("effective_from") val effectiveFrom: String,
    @SerializedName("target_days") val targetDays: List<Int>,
    @SerializedName("is_every_day") val isEveryDay: Boolean
)

data class HabitRemoteCompletion(val date: String, val completed: Boolean)

data class HabitRemoteRecord(
    val id: String,
    val name: String,
    @SerializedName("target_days") val targetDays: List<Int>,
    @SerializedName("is_every_day") val isEveryDay: Boolean,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("scheduled_since") val scheduledSince: String,
    val order: Int,
    @SerializedName("reminder_time") val reminderTime: String?,
    @SerializedName("archived_at") val archivedAt: String?,
    val deleted: Boolean,
    val revisions: List<HabitRemoteRevision>,
    val completions: List<HabitRemoteCompletion>
)

data class HabitSnapshotResponse(
    val version: Long,
    val habits: List<HabitRemoteRecord>,
    val nextCursor: String?,
    val unchanged: Boolean
)

data class HabitBatchRequest(val mutations: List<JsonObject>)
data class HabitBatchResponse(val version: Long, val acknowledged: List<String>)

interface HabitApi {
    @GET("habits/sync")
    suspend fun snapshot(
        @Header("X-Habit-Owner") owner: String,
        @Query("knownVersion") knownVersion: Long? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 500
    ): HabitSnapshotResponse

    @POST("habits/sync/mutations")
    suspend fun mutate(@Header("X-Habit-Owner") owner: String, @Body request: HabitBatchRequest): HabitBatchResponse
}
