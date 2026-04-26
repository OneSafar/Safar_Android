package com.safar.app.ui.nishtha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.domain.model.GoalSubtask
import com.safar.app.domain.repository.HomeRepository
import com.safar.app.domain.repository.JournalRepository
import com.safar.app.domain.repository.NishthaRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NishthaViewModel @Inject constructor(
    private val nishthaRepository: NishthaRepository,
    private val journalRepository: JournalRepository,
    private val homeRepository: HomeRepository,
    val dataStore: SafarDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NishthaUiState())
    val uiState = _uiState.asStateFlow()

    init { loadMoods(); loadJournals(); loadGoals(); loadGoalRolloverPrompts(); loadEkagraAnalytics(); loadStreaks(); loadMonthlyReport(); loadLoginHistory(); loadAchievements() }

    fun onEvent(event: NishthaEvent) {
        when (event) {
            is NishthaEvent.LoadMoods           -> loadMoods()
            is NishthaEvent.CreateMood          -> createMood(event.mood, event.intensity, event.notes)
            is NishthaEvent.ClearCheckInSuccess -> _uiState.update { it.copy(checkInSuccess = false) }
            is NishthaEvent.LoadJournals        -> loadJournals()
            is NishthaEvent.SaveJournal         -> saveJournal(event.content, event.title, event.moodTag)
            is NishthaEvent.ClearJournalSuccess -> _uiState.update { it.copy(journalSaveSuccess = false) }
            is NishthaEvent.LoadGoals           -> loadGoals()
            is NishthaEvent.AddGoal             -> addGoal(event.title, event.description, event.priority, event.scheduledDate, event.startedAt, event.subtasks.mapIndexed { i, text -> GoalSubtask(id = "subtask-$i-${text.hashCode()}", text = text) })
            is NishthaEvent.UpdateGoal          -> updateGoal(event.id, event.title, event.description, event.priority)
            is NishthaEvent.CompleteGoal        -> completeGoal(event.id, event.studiedMinutes)
            is NishthaEvent.DeleteGoal          -> deleteGoal(event.id)
            is NishthaEvent.ClearGoalSuccess    -> _uiState.update { it.copy(goalSaveSuccess = false) }
            is NishthaEvent.LoadStreaks           -> loadStreaks()
            is NishthaEvent.LoadMonthlyReport    -> loadMonthlyReport()
            is NishthaEvent.LoadReportForMonth   -> loadMonthlyReportForMonth(event.month)
            is NishthaEvent.ClearError          -> _uiState.update { it.copy(error = null, checkInError = null, journalError = null, goalError = null) }
        }
    }

    fun createMood(mood: String, intensity: Int, notes: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingIn = true, checkInError = null) }
            when (val r = nishthaRepository.createMood(mood, intensity, notes)) {
                is Resource.Success -> _uiState.update { it.copy(isCheckingIn = false, checkInSuccess = true, moods = listOf(r.data) + it.moods) }
                is Resource.Error   -> _uiState.update { it.copy(isCheckingIn = false, checkInError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    public fun loadMoods() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoods = true) }
            when (val r = nishthaRepository.getMoods()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingMoods = false, moods = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingMoods = false, error = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadJournals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingJournals = true) }
            when (val r = journalRepository.getJournals()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingJournals = false, journals = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingJournals = false, journalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun saveJournal(content: String, title: String?, moodTag: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingJournal = true, journalError = null) }
            when (val r = journalRepository.createJournal(content, title, moodTag)) {
                is Resource.Success -> _uiState.update { it.copy(isSavingJournal = false, journalSaveSuccess = true, journals = listOf(r.data) + it.journals) }
                is Resource.Error   -> _uiState.update { it.copy(isSavingJournal = false, journalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingGoals = true) }
            when (val r = homeRepository.getGoals()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingGoals = false, goals = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingGoals = false, goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun refreshGoals() {
        loadGoals()
        loadGoalRolloverPrompts()
        loadEkagraAnalytics()
    }

    private fun loadGoalRolloverPrompts() {
        viewModelScope.launch {
            when (val r = homeRepository.getRolloverPrompts()) {
                is Resource.Success -> _uiState.update { it.copy(rolloverPrompts = r.data) }
                else -> Unit
            }
        }
    }

    private fun loadEkagraAnalytics() {
        viewModelScope.launch {
            when (val r = homeRepository.getEkagraAnalytics()) {
                is Resource.Success -> _uiState.update { it.copy(ekagraAnalytics = r.data) }
                else -> Unit
            }
        }
    }

    fun addGoal(
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
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.addGoal(title, description, priority, scheduledDate, startedAt, subtasks, goalKind, unitType, linkedFocusEnabled, plannedFocusMinutes, targetValue, achievedValue, status, carryForwardMode, source)) {
                is Resource.Success -> _uiState.update { it.copy(isSavingGoal = false, goalSaveSuccess = true, goals = listOf(r.data) + it.goals) }
                is Resource.Error   -> _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateGoal(id: String, title: String, description: String?, priority: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            val old = _uiState.value.goals.firstOrNull { it.id == id }
            when (val r = homeRepository.updateGoalDetails(
                id = id,
                title = title,
                description = description,
                priority = priority,
                scheduledDate = old?.scheduledDate,
                startedAt = old?.startedAt,
                subtasks = old?.subtasks ?: emptyList(),
                goalKind = old?.goalKind ?: "today",
                unitType = old?.unitType ?: "binary",
                linkedFocusEnabled = old?.linkedFocusEnabled ?: false,
                plannedFocusMinutes = old?.plannedFocusMinutes,
                targetValue = old?.targetValue,
                achievedValue = old?.achievedValue,
                status = old?.status ?: "not_started",
                carryForwardMode = old?.carryForwardMode ?: "none"
            )) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSavingGoal = false,
                            goalSaveSuccess = true,
                            goals = state.goals.map { if (it.id == id) it.copy(title = title, text = title, description = description, priority = priority) else it }
                        )
                    }
                    loadGoals()
                }
                is Resource.Error   -> _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun updateGoalDetails(
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
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.updateGoalDetails(id, title, description, priority, scheduledDate, startedAt, subtasks, goalKind, unitType, linkedFocusEnabled, plannedFocusMinutes, targetValue, achievedValue, status, carryForwardMode)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSavingGoal = false, goalSaveSuccess = true) }
                    loadGoals()
                }
                is Resource.Error -> _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun completeGoal(id: String, studiedMinutes: Int) {
        viewModelScope.launch {
            when (val r = homeRepository.completeGoal(id, studiedMinutes)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        val now = java.time.Instant.now().toString()
                        state.copy(goals = state.goals.map { if (it.id == id) it.copy(completed = true, completedAt = now, studiedMinutes = studiedMinutes, status = "completed") else it })
                    }
                    loadStreaks()
                    loadEkagraAnalytics()
                }
                is Resource.Error   -> {
                    _uiState.update { it.copy(goalError = r.message) }
                    loadGoals()
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            when (val r = homeRepository.deleteGoal(id)) {
                is Resource.Success -> _uiState.update { state ->
                    state.copy(goals = state.goals.filter { it.id != id })
                }
                is Resource.Error   -> _uiState.update { it.copy(goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun repeatGoal(id: String, scheduledDate: String) {
        viewModelScope.launch {
            when (val r = homeRepository.repeatGoal(id, scheduledDate)) {
                is Resource.Success -> _uiState.update { it.copy(goals = listOf(r.data) + it.goals) }
                is Resource.Error -> _uiState.update { it.copy(goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun respondToRollover(id: String, action: String) {
        viewModelScope.launch {
            when (val r = homeRepository.respondToRollover(id, action)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        val newGoal = r.data.goal
                        state.copy(
                            rolloverPrompts = state.rolloverPrompts.filterNot { it.id == id },
                            goals = if (newGoal != null) listOf(newGoal) + state.goals else state.goals
                        )
                    }
                    loadGoals()
                }
                is Resource.Error -> _uiState.update { it.copy(goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadMonthlyReport() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReport = true) }
            when (val r = homeRepository.getMonthlyReport()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingReport = false, monthlyReport = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingReport = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadMonthlyReportForMonth(month: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReport = true, monthlyReport = null) }
            when (val r = homeRepository.generateMonthlyReport(month)) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingReport = false, monthlyReport = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingReport = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadStreaks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStreaks = true) }
            when (val r = homeRepository.getStreaks()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingStreaks = false, streaks = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingStreaks = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadLoginHistory() {
        viewModelScope.launch {
            when (val r = homeRepository.getLoginHistory()) {
                is Resource.Success -> _uiState.update { it.copy(loginHistory = r.data) }
                else -> Unit
            }
        }
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            when (val r = homeRepository.getAchievements()) {
                is Resource.Success -> _uiState.update { it.copy(achievements = r.data) }
                else -> Unit
            }
        }
    }
}
