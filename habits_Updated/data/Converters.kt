package com.safarparmar.app.feature.habits.data

import androidx.room.TypeConverter
import java.time.DayOfWeek
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromDayOfWeekSet(days: Set<DayOfWeek>): String =
        days.joinToString(",") { it.value.toString() }

    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> =
        if (value.isBlank()) emptySet()
        else value.split(",").map { DayOfWeek.of(it.trim().toInt()) }.toSet()
}
