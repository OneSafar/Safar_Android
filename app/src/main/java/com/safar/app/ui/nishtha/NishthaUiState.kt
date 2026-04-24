package com.safar.app.ui.nishtha

import com.safar.app.domain.model.*

data class NishthaUiState(
    val isLoadingMoods: Boolean = false,
    val moods: List<Mood> = emptyList(),
    val isCheckingIn: Boolean = false,
    val checkInSuccess: Boolean = false,
    val checkInError: String? = null,
    val isLoadingJournals: Boolean = false,
    val journals: List<JournalEntry> = emptyList(),
    val isSavingJournal: Boolean = false,
    val journalSaveSuccess: Boolean = false,
    val journalError: String? = null,
    val isLoadingGoals: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val isSavingGoal: Boolean = false,
    val goalSaveSuccess: Boolean = false,
    val goalError: String? = null,
    val isLoadingStreaks: Boolean = false,
    val streaks: Streaks = Streaks(),
    val isLoadingReport: Boolean = false,
    val monthlyReport: MonthlyReport? = null,
    val loginHistory: List<com.safar.app.domain.model.LoginHistoryEntry> = emptyList(),
    val achievements: List<com.safar.app.domain.model.Achievement> = emptyList(),
    val error: String? = null
)

sealed class NishthaEvent {
    object LoadMoods : NishthaEvent()
    data class CreateMood(val mood: String, val intensity: Int, val notes: String?) : NishthaEvent()
    object ClearCheckInSuccess : NishthaEvent()
    object LoadJournals : NishthaEvent()
    data class SaveJournal(val content: String, val title: String?, val moodTag: String?) : NishthaEvent()
    object ClearJournalSuccess : NishthaEvent()
    object LoadGoals : NishthaEvent()
    data class AddGoal(val title: String, val description: String?, val priority: String, val scheduledDate: String, val startedAt: String, val subtasks: List<String> = emptyList()) : NishthaEvent()
    data class UpdateGoal(val id: String, val title: String, val description: String?, val priority: String) : NishthaEvent()
    data class CompleteGoal(val id: String, val studiedMinutes: Int) : NishthaEvent()
    data class DeleteGoal(val id: String) : NishthaEvent()
    object ClearGoalSuccess : NishthaEvent()
    object LoadStreaks : NishthaEvent()
    object LoadMonthlyReport : NishthaEvent()
    data class LoadReportForMonth(val month: String) : NishthaEvent()
    object ClearError : NishthaEvent()
}
