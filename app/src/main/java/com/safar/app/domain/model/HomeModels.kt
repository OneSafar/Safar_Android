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
    val userId: String = "",
    val text: String = "",
    val title: String = "",
    val description: String? = null,
    val source: String = "manual",
    val importedFromGoal: Boolean = false,
    val completedViaFocus: Boolean = false,
    val goalKind: String = "today",
    val unitType: String = "binary",
    val executionMode: String = "manual",
    val linkedFocusEnabled: Boolean = false,
    val plannedFocusMinutes: Int? = null,
    val targetValue: Int? = null,
    val achievedValue: Int = 0,
    val status: String = "not_started",
    val carryForwardMode: String = "none",
    val category: String = "other",
    val priority: String = "medium",
    val completed: Boolean = false,
    val createdAt: String? = null,
    val completedAt: String? = null,
    val studiedMinutes: Int? = null,
    val scheduledDate: String? = null,
    val startedAt: String? = null,
    val expiresAt: String? = null,
    val lifecycleStatus: String? = null,
    val rolloverPromptPending: Boolean = false,
    val sourceGoalId: String? = null,
    val type: String? = null,
    val subtasks: List<GoalSubtask> = emptyList()
)

data class GoalSubtask(
    val id: String = "",
    val text: String = "",
    val done: Boolean = false
)

data class GoalFocusSummary(
    val allTime: Map<String, GoalFocusStats> = emptyMap(),
    val forDay: Map<String, GoalFocusStats> = emptyMap()
)

data class GoalFocusStats(
    val totalMinutes: Int = 0,
    val sessionCount: Int = 0
)

data class GoalRolloverResult(
    val message: String = "",
    val goal: Goal? = null
)

data class RepeatPlanResult(
    val message: String = "",
    val goals: List<Goal> = emptyList()
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

data class EkagraAnalyticsStats(
    val totalFocusMinutes: Int = 0,
    val totalBreakMinutes: Int = 0,
    val timerUsageCount: Int = 0,
    val breakSessionsCount: Int = 0,
    val shortBreakSessionsCount: Int = 0,
    val longBreakSessionsCount: Int = 0,
    val longDurationSessionCount: Int = 0,
    val averageTimerMinutes: Int = 0,
    val mostUsedTimerDurationMinutes: Int? = null,
    val totalSessions: Int = 0,
    val completedSessions: Int = 0,
    val endedEarlySessions: Int = 0,
    val abandonedSessions: Int = 0,
    val weeklyData: List<Int> = List(7) { 0 },
    val weeklyBreaks: List<Int> = List(7) { 0 },
    val focusStreak: Int = 0,
    val hourlyDistribution: List<Int> = List(24) { 0 },
    val recentSessions: List<EkagraAnalyticsRecentSession> = emptyList(),
    val focusSessions: List<EkagraAnalyticsFocusSession> = emptyList()
)

data class EkagraAnalyticsRecentSession(
    val id: String = "",
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val completed: Boolean = false,
    val taskText: String? = null,
    val associatedGoalId: String? = null,
    val pauseCount: Int = 0,
    val sessionType: String = "focus"
)

data class EkagraAnalyticsFocusSession(
    val id: String = "",
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationMinutes: Int = 0,
    val actualMinutes: Int = 0,
    val status: String = "completed",
    val rawStatus: String = "completed",
    val taskText: String? = null,
    val associatedGoalId: String? = null,
    val pauseCount: Int = 0
)
