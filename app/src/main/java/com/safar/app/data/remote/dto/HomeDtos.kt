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
    val category: String? = null,
    val priority: String? = null,
    val subtasks: List<Any>? = null,
    val type: String? = null,
    val completed: Boolean? = false,
    @SerializedName("completed_at")      val completedAtSnake: String? = null,
    @SerializedName("started_at")        val startedAt: String? = null,
    @SerializedName("expires_at")        val expiresAt: String? = null,
    @SerializedName("scheduled_date")    val scheduledDateSnake: String? = null,
    @SerializedName("lifecycle_status")  val lifecycleStatusSnake: String? = null,
    val completedAt: String? = null,
    val scheduledDate: String? = null,
    val lifecycleStatus: String? = null
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
    val subtasks: List<String> = emptyList(),
    val type: String = "daily",
    @SerializedName("scheduled_date") val scheduledDate: String,
    val source: String = "manual",
    @SerializedName("started_at") val startedAt: String
)

data class UpdateGoalRequest(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null
)

data class CompleteGoalRequest(
    val completed: Boolean = true,
    @SerializedName("completed_at")   val completedAt: String,
    @SerializedName("studied_minutes") val studiedMinutes: Int
)
