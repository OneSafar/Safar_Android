package com.safar.app.domain.repository

import com.safar.app.domain.model.*
import com.safar.app.util.Resource

interface HomeRepository {
    suspend fun getStreaks(): Resource<Streaks>
    suspend fun getMoods(): Resource<List<Mood>>
    suspend fun getGoals(): Resource<List<Goal>>
    suspend fun addGoal(
        title: String,
        description: String?,
        priority: String,
        scheduledDate: String?,
        startedAt: String?,
        subtasks: List<GoalSubtask>,
        goalKind: String = "today",
        unitType: String = "binary",
        linkedFocusEnabled: Boolean = false,
        plannedFocusMinutes: Int? = null,
        targetValue: Int? = null,
        achievedValue: Int? = null,
        status: String = "not_started",
        carryForwardMode: String = "none",
        source: String = "manual"
    ): Resource<Goal>
    suspend fun updateGoalDetails(
        id: String,
        title: String,
        description: String?,
        priority: String,
        scheduledDate: String?,
        startedAt: String?,
        subtasks: List<GoalSubtask>,
        goalKind: String,
        unitType: String,
        linkedFocusEnabled: Boolean,
        plannedFocusMinutes: Int?,
        targetValue: Int?,
        achievedValue: Int?,
        status: String,
        carryForwardMode: String
    ): Resource<Unit>
    suspend fun completeGoal(id: String, studiedMinutes: Int): Resource<Unit>
    suspend fun deleteGoal(id: String): Resource<Unit>
    suspend fun repeatGoal(id: String, scheduledDate: String): Resource<Goal>
    suspend fun getRolloverPrompts(): Resource<List<Goal>>
    suspend fun respondToRollover(id: String, action: String): Resource<GoalRolloverResult>
    suspend fun getGoalFocusSummary(goalIds: List<String>, dayKey: String?): Resource<GoalFocusSummary>
    suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats>
    suspend fun getMonthlyReport(): Resource<MonthlyReport>
    suspend fun generateMonthlyReport(month: String): Resource<MonthlyReport>
    suspend fun getActiveTitle(): Resource<ActiveTitle>
    suspend fun getAchievements(): Resource<List<Achievement>>
    suspend fun getLoginHistory(): Resource<List<LoginHistoryEntry>>
}
