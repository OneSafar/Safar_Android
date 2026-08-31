package com.safarparmar.app.domain.repository

import com.safarparmar.app.domain.model.*
import com.safarparmar.app.util.Resource

interface HomeRepository {
    suspend fun getStreaks(): Resource<Streaks>
    suspend fun restoreCheckInStreak(): Resource<Streaks>
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
    suspend fun completeGoal(
        id: String,
        studiedMinutes: Int,
        studiedSeconds: Int = 0,
        scheduledDate: String? = null,
        completedViaFocus: Boolean = false,
    ): Resource<Unit>
    suspend fun deleteGoal(id: String): Resource<Unit>
    suspend fun getRecentlyDeletedGoals(): Resource<List<Goal>>
    suspend fun restoreGoal(id: String): Resource<Goal>
    suspend fun reopenGoal(id: String): Resource<Goal>
    suspend fun repeatGoal(id: String, scheduledDate: String): Resource<Goal>

    /** Copies several goals onto today in one write. The server skips any that are
     *  already present, so this is safe to call twice. */
    suspend fun repeatGoals(goalIds: List<String>): Resource<RepeatPlanResult>
    suspend fun getRolloverPrompts(): Resource<List<Goal>>
    suspend fun respondToRollover(id: String, action: String): Resource<GoalRolloverResult>
    suspend fun getGoalFocusSummary(goalIds: List<String>, dayKey: String?): Resource<GoalFocusSummary>
    suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats>
    suspend fun getMonthlyReport(): Resource<MonthlyReport>
    suspend fun generateMonthlyReport(month: String): Resource<MonthlyReport>
    suspend fun getActiveTitle(): Resource<ActiveTitle>
    suspend fun getAchievements(): Resource<List<Achievement>>
    suspend fun selectAchievement(achievementId: String?): Resource<ActiveTitle>
    suspend fun trackDhyanSession(durationMinutes: Int, source: String = "android"): Resource<Unit>
    suspend fun trackKavachEvent(eventType: String, blockedAppCount: Int, sessionId: String? = null): Resource<Unit>
    suspend fun getLoginHistory(): Resource<List<LoginHistoryEntry>>
}
