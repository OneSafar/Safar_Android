package com.safarparmar.app.feature.habits.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

/** The wire format is stored unchanged in Room before any network request. */
internal object HabitMutationJson {
    fun create(habit: HabitEntity): JsonObject = base("create", habit.remoteId!!).apply {
        addProperty("name", habit.name)
        add("targetDays", days(habit.targetDays))
        addProperty("isEveryDay", habit.isEveryDay)
        addProperty("isActive", habit.isActive)
        addProperty("scheduledSince", habit.scheduledSince.toString())
        addProperty("order", habit.order)
        if (habit.reminderTime == null) add("reminderTime", com.google.gson.JsonNull.INSTANCE)
        else addProperty("reminderTime", habit.reminderTime)
    }

    fun patch(remoteId: String, fields: JsonObject): JsonObject = base("patch", remoteId).apply {
        add("fields", fields)
    }

    fun schedule(remoteId: String, date: LocalDate, targetDays: Set<DayOfWeek>, isEveryDay: Boolean): JsonObject =
        base("schedule", remoteId).apply {
            addProperty("effectiveFrom", date.toString())
            add("targetDays", days(targetDays))
            addProperty("isEveryDay", isEveryDay)
        }

    fun completion(remoteId: String, date: LocalDate, completed: Boolean): JsonObject =
        base("completion", remoteId).apply {
            addProperty("date", date.toString())
            addProperty("completed", completed)
        }

    fun archive(remoteId: String, date: LocalDate): JsonObject = base("archive", remoteId).apply {
        addProperty("archivedAt", date.toString())
    }

    fun delete(remoteId: String): JsonObject = base("delete", remoteId)

    fun decode(payload: String): JsonObject = JsonParser.parseString(payload).asJsonObject

    private fun base(type: String, remoteId: String) = JsonObject().apply {
        addProperty("type", type)
        addProperty("mutationId", UUID.randomUUID().toString())
        addProperty("habitId", remoteId)
    }

    private fun days(days: Set<DayOfWeek>) = com.google.gson.JsonArray().apply {
        days.sortedBy { it.value }.forEach { add(it.value) }
    }
}
