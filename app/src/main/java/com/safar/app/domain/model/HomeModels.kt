package com.safar.app.domain.model

data class Streaks(
    val loginStreak: Int = 0,
    val checkInStreak: Int = 0,
    val goalCompletionStreak: Int = 0,
    val lastActiveDate: String? = null
)

data class Mood(
    val id: String = "",
    val mood: String = "",
    val intensity: Int = 0,
    val notes: String? = null,
    val timestamp: String = ""
)

data class Goal(
    val id: String = "",
    val title: String = "",
    val description: String? = null,
    val category: String = "other",
    val priority: String = "medium",
    val completed: Boolean = false,
    val completedAt: String? = null,
    val scheduledDate: String? = null,
    val lifecycleStatus: String? = null,
    val type: String? = null,
    val startedAt: String? = null
)

data class JournalEntry(
    val id: String = "",
    val content: String = "",
    val timestamp: String = ""
)

data class MonthlyReport(
    val month: String = "",
    val generatedAt: String = "",
    val consistencyScore: Double = 0.0,
    val completionRate: Double = 0.0,
    val focusDepth: Double = 0.0,
    val daysLoggedIn: Int = 0,
    val daysInMonth: Int = 31,
    val goalsCreated: Int = 0,
    val goalsCompleted: Int = 0,
    val totalFocusMinutes: Int = 0,
    val consistencyMessage: String = "",
    val completionMessage: String = "",
    val focusMessage: String = "",
    val powerHourMessage: String = "",
    val moodConnectionMessage: String = "",
    val sundayScariesMessage: String = "",
    val radar: List<RadarItem> = emptyList(),
    val heatmap: List<HeatmapDay> = emptyList()
)

data class RadarItem(val subject: String = "", val score: Double = 0.0, val fullMark: Int = 100)
data class HeatmapDay(val date: String = "", val dayOfWeek: String = "", val value: Int = 0, val intensity: Int = 0)
data class ActiveTitle(val title: String = "", val selectedId: String = "")

data class Achievement(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val type: String = "",
    val category: String = "",
    val rarity: String? = null,
    val tier: Int? = null,
    val requirement: String = "",
    val holderCount: Int = 0,
    val earned: Boolean = false,
    val progress: Int = 0,
    val currentValue: Int = 0,
    val targetValue: Int = 0
)

data class LoginHistoryEntry(val timestamp: String = "")
