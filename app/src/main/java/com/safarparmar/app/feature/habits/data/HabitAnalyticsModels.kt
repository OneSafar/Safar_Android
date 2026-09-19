package com.safarparmar.app.feature.habits.data

import java.time.LocalDate

data class HabitPeriodStats(
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val activeDays: Int,
    val perfectDays: Int
)

data class HabitPerformance(
    val habitId: Long,
    val habitName: String,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float,
    val currentStreak: Int,
    val bestStreak: Int,
    val isArchived: Boolean = false
)

data class DailyHabitStats(
    val date: LocalDate,
    val scheduledCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class HabitAnalytics(
    val currentStreak: Int,
    val bestStreak: Int,
    val periodStats: HabitPeriodStats,
    val dailyStats: List<DailyHabitStats>,
    val habitPerformance: List<HabitPerformance>
)
