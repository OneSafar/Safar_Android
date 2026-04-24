package com.safar.app.ui.dashboard

import com.safar.app.domain.model.*

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userAvatar: String? = null,
    val activeTitle: String = "",
    val activeTitleId: String = "",
    val streaks: Streaks = Streaks(),
    val todayMood: Mood? = null,
    val todayGoals: List<Goal> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
    val monthlyReport: MonthlyReport? = null,
    val weeklyMoods: List<Mood> = emptyList(),
    val earnedAchievements: List<Achievement> = emptyList(),
    val allAchievements: List<Achievement> = emptyList(),
    val loginHistory: List<LoginHistoryEntry> = emptyList(),
    val error: String? = null,
    val showWelcomeOverlay: Boolean = false
)

sealed class DashboardEvent {
    object Refresh : DashboardEvent()
    object ClearError : DashboardEvent()
}
