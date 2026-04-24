package com.safar.app.data.remote.dto


data class EkagraSession(
    val id: String,
    val userId: String,
    val goalId: String?,
    val goalTitle: String?,
    val sessionType: String,
    val sessionTitle: String?,
    val source: String,
    val status: String,
    val mode: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val importedFromGoal: Boolean,
    val pauseCount: Int,
    val sessionStartedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val completedAt: String?,
    val endedAt: String?,
    val discardedAt: String?,
)


data class EkagraSessionsResponse(
    val sessions: List<EkagraSession>,
)


data class ActivateSessionRequest(
    val sessionType: String = "named",
    val sessionTitle: String,
    val source: String = "manual",
    val overrideActive: Boolean = true,
    val mode: String = "Timer",
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean = true,
    val sessionStartedAt: String,
)

data class ActivateSessionResponse(
    val session: EkagraSession,
)


data class CompleteSessionRequest(
    val mode: String = "Timer",
    val totalSeconds: Int,
    val elapsedSeconds: Int,
    val remainingSeconds: Int,
    val sessionStartedAt: String?,
)

data class CompleteSessionResponse(
    val session: EkagraSession,
)

data class FocusStatsResponse(
    val totalFocusMinutes: Int,
    val totalBreakMinutes: Int,
    val totalSessions: Int,
    val completedSessions: Int,
    val weeklyData: List<Int>,
    val weeklyBreaks: List<Int>,
    val focusStreak: Int,
    val goalsSet: Int,
    val goalsCompleted: Int,
    val dailyGoalMinutes: Int,
    val dailyGoalProgress: Int,
    val hourlyDistribution: List<Int>,
    val recentSessions: List<RecentSession>,
)

data class RecentSession(
    val id: String,
    val startedAt: String,
    val durationMinutes: Int,
    val actualMinutes: Int,
    val completed: Boolean,
    val taskText: String?,
)
