package com.safar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// Both camelCase and snake_case variants are present to handle inconsistent API responses
data class StreaksDto(
    val loginStreak: Int? = null,
    @SerializedName("login_streak")              val loginStreakSnake: Int? = null,
    val checkInStreak: Int? = null,
    @SerializedName("check_in_streak")           val checkInStreakSnake: Int? = null,
    val goalCompletionStreak: Int? = null,
    @SerializedName("goal_completion_streak")    val goalCompletionStreakSnake: Int? = null,
    val lastActiveDate: String? = null,
    @SerializedName("last_active_date")          val lastActiveDateSnake: String? = null
)

data class MoodDto(
    val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    val mood: String? = null,
    val intensity: Int? = null,
    val notes: String? = null,
    val timestamp: String? = null
)

data class CreateMoodRequest(val mood: String, val intensity: Int, val notes: String?)

// Both id and _id variants handled for MongoDB compatibility
data class GoalDto(
    @SerializedName("_id")              val mongoId: String? = null,
    val id: String? = null,
    @SerializedName("user_id")          val userId: String? = null,
    val text: String? = null,
    val title: String? = null,
    val description: String? = null,
    val source: String? = null,
    val importedFromGoal: Boolean? = null,
    @SerializedName("imported_from_goal") val importedFromGoalSnake: Boolean? = null,
    val completedViaFocus: Boolean? = null,
    @SerializedName("completed_via_focus") val completedViaFocusSnake: Boolean? = null,
    val goalKind: String? = null,
    @SerializedName("goal_kind") val goalKindSnake: String? = null,
    val unitType: String? = null,
    @SerializedName("unit_type") val unitTypeSnake: String? = null,
    val executionMode: String? = null,
    @SerializedName("execution_mode") val executionModeSnake: String? = null,
    val linkedFocusEnabled: Boolean? = null,
    @SerializedName("linked_focus_enabled") val linkedFocusEnabledSnake: Boolean? = null,
    val plannedFocusMinutes: Int? = null,
    @SerializedName("planned_focus_minutes") val plannedFocusMinutesSnake: Int? = null,
    val targetValue: Int? = null,
    @SerializedName("target_value") val targetValueSnake: Int? = null,
    val achievedValue: Int? = null,
    @SerializedName("achieved_value") val achievedValueSnake: Int? = null,
    val status: String? = null,
    @SerializedName("status_value") val statusSnake: String? = null,
    val carryForwardMode: String? = null,
    @SerializedName("carry_forward_mode") val carryForwardModeSnake: String? = null,
    val category: String? = null,
    val priority: String? = null,
    val subtasks: List<Any>? = null,
    val type: String? = null,
    val completed: Boolean? = false,
    val createdAt: String? = null,
    @SerializedName("created_at")        val createdAtSnake: String? = null,
    @SerializedName("completed_at")      val completedAtSnake: String? = null,
    val studiedMinutes: Int? = null,
    @SerializedName("studied_minutes")   val studiedMinutesSnake: Int? = null,
    @SerializedName("started_at")        val startedAt: String? = null,
    @SerializedName("startedAt")         val startedAtCamel: String? = null,
    @SerializedName("expires_at")        val expiresAt: String? = null,
    @SerializedName("expiresAt")         val expiresAtCamel: String? = null,
    @SerializedName("scheduled_date")    val scheduledDateSnake: String? = null,
    @SerializedName("lifecycle_status")  val lifecycleStatusSnake: String? = null,
    @SerializedName("rollover_prompt_pending") val rolloverPromptPendingSnake: Boolean? = null,
    @SerializedName("source_goal_id")     val sourceGoalIdSnake: String? = null,
    val completedAt: String? = null,
    val scheduledDate: String? = null,
    val lifecycleStatus: String? = null
)

data class GoalSubtaskDto(
    val id: String? = null,
    val text: String? = null,
    val done: Boolean? = false
)

data class JournalDto(
    @SerializedName("_id")     val mongoId: String? = null,
    val id: String? = null,
    @SerializedName("user_id") val userId: String? = null,
    val content: String? = null,
    val timestamp: String? = null
)

data class CreateJournalRequest(val content: String, val title: String? = null, val moodTag: String? = null)

data class MonthlyReportDto(
    val month: String? = null,
    val generatedAt: String? = null,
    val executiveSummary: ExecutiveSummaryDto? = null,
    val insights: InsightsDto? = null,
    val radar: List<RadarItemDto>? = null,
    val heatmap: List<HeatmapDayDto>? = null
)

data class ExecutiveSummaryDto(
    val consistencyScore: Double? = 0.0,
    val completionRate: Double? = 0.0,
    val focusDepth: Double? = 0.0,
    val daysLoggedIn: Int? = 0,
    val daysInMonth: Int? = 31,
    val goalsCreated: Int? = 0,
    val goalsCompleted: Int? = 0,
    val totalFocusMinutes: Int? = 0,
    val consistencyMessage: String? = "",
    val completionMessage: String? = "",
    val focusMessage: String? = ""
)

data class InsightsDto(
    val powerHour: PowerHourDto? = null,
    val moodConnection: MoodConnectionDto? = null,
    val sundayScaries: SundayScariesDto? = null
)

data class PowerHourDto(val startHour: Int? = null, val endHour: Int? = null, val message: String? = null)
data class MoodConnectionDto(val message: String? = null)
data class SundayScariesDto(val weakestDay: String? = null, val message: String? = null)

data class RadarItemDto(val subject: String? = null, val score: Double? = 0.0, val fullMark: Int? = 100)
data class HeatmapDayDto(val date: String? = null, val dayOfWeek: String? = null, val value: Int? = 0, val intensity: Int? = 0)

data class GenerateReportRequest(val month: String)

data class AchievementsResponse(val achievements: List<AchievementDto>? = null)

data class AchievementDto(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val rarity: String? = null,
    val tier: Int? = null,
    val requirement: String? = null,
    val holderCount: Int? = 0,
    val earned: Boolean? = false,
    val progress: Int? = 0,
    val currentValue: Int? = 0,
    val targetValue: Int? = 0
)

data class ActiveTitleDto(val title: String? = null, val selectedId: String? = null)

data class FocusSessionsByGoalsRequest(val goalIds: List<String>)

data class FocusSessionDto(val id: String? = null, val goalId: String? = null, val duration: Int? = 0, val completedAt: String? = null)

data class AddGoalRequest(
    val text: String,
    val title: String,
    val description: String? = null,
    val priority: String? = null,
    val subtasks: List<GoalSubtaskDto> = emptyList(),
    val type: String = "daily",
    val scheduledDate: String? = null,
    val source: String = "manual",
    val startedAt: String? = null,
    val goalKind: String? = null,
    val unitType: String? = null,
    val executionMode: String? = null,
    val linkedFocusEnabled: Boolean? = null,
    val plannedFocusMinutes: Int? = null,
    val targetValue: Int? = null,
    val achievedValue: Int? = null,
    val status: String? = null,
    val carryForwardMode: String? = null
)

data class UpdateGoalRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val text: String? = null,
    val subtasks: List<GoalSubtaskDto>? = null,
    val scheduledDate: String? = null,
    val startedAt: String? = null,
    val goalKind: String? = null,
    val unitType: String? = null,
    val executionMode: String? = null,
    val linkedFocusEnabled: Boolean? = null,
    val plannedFocusMinutes: Int? = null,
    val targetValue: Int? = null,
    val achievedValue: Int? = null,
    val status: String? = null,
    val carryForwardMode: String? = null
)

data class CompleteGoalRequest(
    val completed: Boolean = true,
    val completedAt: String,
    val studiedMinutes: Int
)

data class RepeatGoalRequest(val scheduledDate: String)
data class RepeatPlanRequest(val goalIds: List<String>)
data class RolloverActionRequest(val action: String)
data class FocusSummaryRequest(val goalIds: List<String>, val dayKey: String? = null)

data class BasicMessageResponse(
    val message: String? = null,
    val completed: Boolean? = null,
    val completedAt: String? = null
)

data class RepeatPlanResponse(val message: String? = null, val goals: List<GoalDto>? = null)
data class RolloverActionResponse(val message: String? = null, val goal: GoalDto? = null)
data class GoalFocusSummaryResponse(
    val allTime: Map<String, GoalFocusSummaryItemDto>? = null,
    val forDay: Map<String, GoalFocusSummaryItemDto>? = null
)
data class GoalFocusSummaryItemDto(val totalMinutes: Int? = 0, val sessionCount: Int? = 0)

data class EkagraAnalyticsStatsDto(
    val totalFocusMinutes: Int? = 0,
    val totalBreakMinutes: Int? = 0,
    val timerUsageCount: Int? = 0,
    val breakSessionsCount: Int? = 0,
    val shortBreakSessionsCount: Int? = 0,
    val longBreakSessionsCount: Int? = 0,
    val longDurationSessionCount: Int? = 0,
    val averageTimerMinutes: Int? = 0,
    val mostUsedTimerDurationMinutes: Int? = null,
    val totalSessions: Int? = 0,
    val completedSessions: Int? = 0,
    val endedEarlySessions: Int? = 0,
    val abandonedSessions: Int? = 0,
    val weeklyData: List<Int>? = null,
    val weeklyBreaks: List<Int>? = null,
    val focusStreak: Int? = 0,
    val hourlyDistribution: List<Int>? = null,
    val recentSessions: List<EkagraAnalyticsRecentSessionDto>? = null,
    val focusSessions: List<EkagraAnalyticsFocusSessionDto>? = null
)

data class EkagraAnalyticsRecentSessionDto(
    val id: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationMinutes: Int? = 0,
    val actualMinutes: Int? = 0,
    val completed: Boolean? = false,
    val taskText: String? = null,
    val associatedGoalId: String? = null,
    val pauseCount: Int? = 0,
    val sessionType: String? = null
)

data class EkagraAnalyticsFocusSessionDto(
    val id: String? = null,
    val startedAt: String? = null,
    val endedAt: String? = null,
    val durationMinutes: Int? = 0,
    val actualMinutes: Int? = 0,
    val status: String? = null,
    val rawStatus: String? = null,
    val taskText: String? = null,
    val associatedGoalId: String? = null,
    val pauseCount: Int? = 0
)
