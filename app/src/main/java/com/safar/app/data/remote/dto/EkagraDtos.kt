package com.safar.app.data.remote.dto

import com.google.gson.annotations.SerializedName


data class EkagraSession(
    val id: String = "",
    val userId: String = "",
    @SerializedName("user_id") val userIdSnake: String? = null,
    val goalId: String? = null,
    @SerializedName("goal_id") val goalIdSnake: String? = null,
    val goalTitle: String? = null,
    @SerializedName("goal_title") val goalTitleSnake: String? = null,
    val sessionType: String = "named",
    @SerializedName("session_type") val sessionTypeSnake: String? = null,
    val sessionTitle: String? = null,
    @SerializedName("session_title") val sessionTitleSnake: String? = null,
    val source: String = "manual",
    val status: String = "paused",
    val mode: String = "Timer",
    val totalSeconds: Int = 25 * 60,
    @SerializedName("total_seconds") val totalSecondsSnake: Int? = null,
    val remainingSeconds: Int = 25 * 60,
    @SerializedName("remaining_seconds") val remainingSecondsSnake: Int? = null,
    val isRunning: Boolean = false,
    @SerializedName("is_running") val isRunningSnake: Boolean? = null,
    val importedFromGoal: Boolean = false,
    @SerializedName("imported_from_goal") val importedFromGoalSnake: Boolean? = null,
    val pauseCount: Int = 0,
    @SerializedName("pause_count") val pauseCountSnake: Int? = null,
    val sessionStartedAt: String? = null,
    @SerializedName("session_started_at") val sessionStartedAtSnake: String? = null,
    val createdAt: String = "",
    @SerializedName("created_at") val createdAtSnake: String? = null,
    val updatedAt: String = "",
    @SerializedName("updated_at") val updatedAtSnake: String? = null,
    val completedAt: String? = null,
    @SerializedName("completed_at") val completedAtSnake: String? = null,
    val endedAt: String? = null,
    @SerializedName("ended_at") val endedAtSnake: String? = null,
    val discardedAt: String? = null,
    @SerializedName("discarded_at") val discardedAtSnake: String? = null,
)


data class EkagraSessionsResponse(
    val sessions: List<EkagraSession>,
)


data class ActivateSessionRequest(
    val sessionType: String = "named",
    val goalId: String? = null,
    val goalTitle: String? = null,
    val sessionTitle: String,
    val source: String = "manual",
    val importedFromGoal: Boolean = false,
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

data class UpdateEkagraSessionRequest(
    val status: String? = null,
    val mode: String? = null,
    val totalSeconds: Int? = null,
    val remainingSeconds: Int? = null,
    val isRunning: Boolean? = null,
    val sessionStartedAt: String? = null,
    val goalTitle: String? = null,
    val source: String? = null,
    val importedFromGoal: Boolean? = null,
)

data class UpdateEkagraSessionResponse(
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

data class DiscardEkagraSessionResponse(
    val session: EkagraSession,
)

data class DeleteEkagraSessionResponse(
    val ok: Boolean = false,
    val deletedId: String? = null,
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
