package com.safar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class HomeStreaksDto(
    @SerializedName("loginStreak")          val loginStreak: Int = 0,
    @SerializedName("checkInStreak")        val checkInStreak: Int = 0,
    @SerializedName("goalCompletionStreak") val goalCompletionStreak: Int = 0,
    @SerializedName("lastActiveDate")       val lastActiveDate: String? = null
)

data class MoodDto(
    @SerializedName("id")        val id: String = "",
    @SerializedName("mood")      val mood: String = "",
    @SerializedName("intensity") val intensity: Int = 0,
    @SerializedName("notes")     val notes: String? = null,
    @SerializedName("timestamp") val timestamp: String = ""
)

data class GoalDto(
    @SerializedName("id")               val id: String = "",
    @SerializedName("title")            val title: String = "",
    @SerializedName("description")      val description: String? = null,
    @SerializedName("category")         val category: String = "",
    @SerializedName("priority")         val priority: String = "",
    @SerializedName("completed")        val completed: Boolean = false,
    @SerializedName("completedAt")      val completedAt: String? = null,
    @SerializedName("scheduledDate")    val scheduledDate: String? = null,
    @SerializedName("lifecycleStatus")  val lifecycleStatus: String = ""
)

data class MonthlyReportDto(
    @SerializedName("month")            val month: String = "",
    @SerializedName("executiveSummary") val executiveSummary: ExecutiveSummaryDto = ExecutiveSummaryDto(),
    @SerializedName("insights")         val insights: InsightsDto = InsightsDto(),
    @SerializedName("radar")            val radar: List<RadarDto> = emptyList()
)

data class ExecutiveSummaryDto(
    @SerializedName("consistencyScore")    val consistencyScore: Double = 0.0,
    @SerializedName("completionRate")      val completionRate: Double = 0.0,
    @SerializedName("focusDepth")          val focusDepth: Double = 0.0,
    @SerializedName("daysLoggedIn")        val daysLoggedIn: Int = 0,
    @SerializedName("daysInMonth")         val daysInMonth: Int = 0,
    @SerializedName("goalsCreated")        val goalsCreated: Int = 0,
    @SerializedName("goalsCompleted")      val goalsCompleted: Int = 0,
    @SerializedName("consistencyMessage")  val consistencyMessage: String = "",
    @SerializedName("completionMessage")   val completionMessage: String = "",
    @SerializedName("focusMessage")        val focusMessage: String = ""
)

data class InsightsDto(
    @SerializedName("powerHour") val powerHour: PowerHourDto = PowerHourDto()
)

data class PowerHourDto(
    @SerializedName("startHour") val startHour: Int = 0,
    @SerializedName("endHour")   val endHour: Int = 0,
    @SerializedName("message")   val message: String = ""
)

data class RadarDto(
    @SerializedName("subject")  val subject: String = "",
    @SerializedName("score")    val score: Double = 0.0,
    @SerializedName("fullMark") val fullMark: Int = 100
)

data class ActiveTitleDto(
    @SerializedName("title")      val title: String = "",
    @SerializedName("type")       val type: String = "",
    @SerializedName("selectedId") val selectedId: String = ""
)

data class AchievementDto(
    @SerializedName("id")           val id: String = "",
    @SerializedName("name")         val name: String = "",
    @SerializedName("description")  val description: String? = null,
    @SerializedName("type")         val type: String = "",
    @SerializedName("category")     val category: String = "",
    @SerializedName("tier")         val tier: Int? = null,
    @SerializedName("rarity")       val rarity: String? = null,
    @SerializedName("requirement")  val requirement: String = "",
    @SerializedName("holderCount")  val holderCount: Int = 0,
    @SerializedName("earned")       val earned: Boolean = false,
    @SerializedName("progress")     val progress: Int = 0,
    @SerializedName("currentValue") val currentValue: Int = 0,
    @SerializedName("targetValue")  val targetValue: Int = 0
)

data class AchievementsResponse(
    @SerializedName("achievements") val achievements: List<AchievementDto> = emptyList()
)