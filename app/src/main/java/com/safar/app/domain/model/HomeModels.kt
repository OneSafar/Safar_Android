package com.safar.app.domain.model

data class Streaks(
    val loginStreak: Int,
    val checkInStreak: Int,
    val goalCompletionStreak: Int
)

data class Mood(
    val id: String,
    val mood: String,
    val intensity: Int,
    val notes: String?,
    val timestamp: String
)

data class Goal(
    val id: String,
    val title: String,
    val description: String?,
    val category: String,
    val priority: String,
    val completed: Boolean,
    val completedAt: String?,
    val scheduledDate: String?
)

data class MonthlyReport(
    val month: String,
    val consistencyScore: Double,
    val completionRate: Double,
    val focusDepth: Double,
    val daysLoggedIn: Int,
    val daysInMonth: Int,
    val goalsCreated: Int,
    val goalsCompleted: Int,
    val consistencyMessage: String,
    val completionMessage: String,
    val focusMessage: String,
    val powerHourMessage: String,
    val radar: List<RadarItem>
)

data class RadarItem(val subject: String, val score: Double)

data class ActiveTitle(val title: String, val selectedId: String)

data class Achievement(
    val id: String,
    val name: String,
    val description: String?,
    val type: String,
    val category: String,
    val tier: Int?,
    val requirement: String,
    val holderCount: Int,
    val earned: Boolean,
    val progress: Int,
    val currentValue: Int,
    val targetValue: Int
)