package com.safarparmar.app.feature.habits.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonNull
import java.time.DayOfWeek
import java.time.LocalDate

object HabitMutationJson {

    fun createHabit(mutationId: String, habit: HabitEntity): String {
        val json = JsonObject()
        json.addProperty("type", "create")
        json.addProperty("mutationId", mutationId)
        json.addProperty("habitId", habit.remoteId)
        json.addProperty("name", habit.name)
        json.addProperty("isEveryDay", habit.isEveryDay)
        json.addProperty("isActive", habit.isActive)
        json.addProperty("scheduledSince", habit.scheduledSince.toString())
        json.addProperty("order", habit.order)
        
        val targetDaysArray = JsonArray()
        habit.targetDays.forEach { targetDaysArray.add(it.value) }
        json.add("targetDays", targetDaysArray)
        
        return json.toString()
    }

    fun patchHabit(mutationId: String, habitId: String, changedFields: Map<String, Any?>): String {
        val json = JsonObject()
        json.addProperty("type", "patch")
        json.addProperty("mutationId", mutationId)
        json.addProperty("habitId", habitId)
        
        val fields = JsonObject()
        changedFields.forEach { (key, value) ->
            when (value) {
                is String -> fields.addProperty(key, value)
                is Boolean -> fields.addProperty(key, value)
                is Number -> fields.addProperty(key, value)
                null -> fields.add(key, JsonNull.INSTANCE)
                is Set<*> -> {
                    val array = JsonArray()
                    value.forEach { 
                        if (it is DayOfWeek) {
                            array.add(it.value)
                        } else {
                            array.add(it?.toString())
                        }
                    }
                    fields.add(key, array)
                }
                else -> fields.addProperty(key, value.toString())
            }
        }
        json.add("fields", fields)
        
        return json.toString()
    }

    fun scheduleRevision(
        mutationId: String, 
        habitId: String, 
        effectiveFrom: LocalDate, 
        targetDays: Set<DayOfWeek>, 
        isEveryDay: Boolean
    ): String {
        val json = JsonObject()
        json.addProperty("type", "schedule")
        json.addProperty("mutationId", mutationId)
        json.addProperty("habitId", habitId)
        json.addProperty("effectiveFrom", effectiveFrom.toString())
        json.addProperty("isEveryDay", isEveryDay)
        
        val targetDaysArray = JsonArray()
        targetDays.forEach { targetDaysArray.add(it.value) }
        json.add("targetDays", targetDaysArray)
        
        return json.toString()
    }

    fun completion(mutationId: String, habitId: String, date: LocalDate, completed: Boolean): String {
        val json = JsonObject()
        json.addProperty("type", "completion")
        json.addProperty("mutationId", mutationId)
        json.addProperty("habitId", habitId)
        json.addProperty("date", date.toString())
        json.addProperty("completed", completed)
        
        return json.toString()
    }
}
