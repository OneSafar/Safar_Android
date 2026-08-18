package com.safarparmar.app.ui.nishtha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.GoalSubtask
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.JournalRepository
import com.safarparmar.app.domain.repository.NishthaRepository
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.assignedDateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@HiltViewModel
class NishthaViewModel @Inject constructor(
    private val nishthaRepository: NishthaRepository,
    private val journalRepository: JournalRepository,
    private val homeRepository: HomeRepository,
    val dataStore: SafarDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NishthaUiState())
    val uiState = _uiState.asStateFlow()

    /** Captures offline/time-out failures that never reach server telemetry.
     * Goal text and study details are deliberately excluded. */
    private fun recordGoalOperationFailure(operation: String) {
        FirebaseCrashlytics.getInstance().apply {
            setCustomKey("goal_operation", operation)
            recordException(IllegalStateException("Goal operation failed: $operation"))
        }
    }

    // Monthly report is NOT loaded here — it's month-scoped and only the Analytics
    // screen's Monthly Review tab needs it (via LoadReportForMonth). Loading it
    // unconditionally for every Nishtha open, even on tabs that never show it, was
    // a wasted network call on every launch.
    init {
        observeGoalEvents()
        loadMoods()
        loadJournals()
        loadGoals()
        loadGoalRolloverPrompts()
        loadEkagraAnalytics()
        loadStreaks()
        loadLoginHistory()
        loadAchievements()
    }

    private fun observeGoalEvents() {
        viewModelScope.launch {
            com.safarparmar.app.ui.nishtha.goals.GoalEventBus.goalUpdatedFromEkagra.collect {
                loadGoals()
            }
        }
    }

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
            is NishthaEvent.CompleteGoal        -> completeGoal(event.id, event.studiedMinutes, event.studiedSeconds)
            is NishthaEvent.DeleteGoal          -> deleteGoal(event.id)
            is NishthaEvent.ClearGoalSuccess    -> _uiState.update { it.copy(goalSaveSuccess = false) }
            is NishthaEvent.LoadStreaks           -> loadStreaks()
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
                is Resource.Success -> {
                    _uiState.update { it.copy(isLoadingGoals = false, goals = r.data) }
                }
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
                is Resource.Success -> _uiState.update {
                    it.copy(
                        isSavingGoal = false,
                        goals = listOf(r.data) + it.goals,
                        goalMessage = "Goal created",
                        goalAction = "create",
                    )
                }
                is Resource.Error   -> {
                    recordGoalOperationFailure("create")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
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
                            goalMessage = "Goal updated",
                            goalAction = "update",
                            goals = state.goals.map { if (it.id == id) it.copy(title = title, text = title, description = description, priority = priority) else it }
                        )
                    }
                    loadGoals()
                }
                is Resource.Error   -> {
                    recordGoalOperationFailure("edit")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
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
                    _uiState.update {
                        it.copy(
                            isSavingGoal = false,
                            goalMessage = "Goal updated",
                            goalAction = "update",
                        )
                    }
                    loadGoals()
                }
                is Resource.Error -> {
                    recordGoalOperationFailure("edit")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun completeGoal(
        id: String,
        studiedMinutes: Int,
        studiedSeconds: Int = 0,
        scheduledDate: String? = null,
    ) {
        viewModelScope.launch {
            val existingGoal = _uiState.value.goals.firstOrNull { it.id == id }
            val historyDateKey = scheduledDate
                ?: existingGoal?.assignedDateKey()
                ?: IstDateUtils.todayKey()
            val historyDateLabel = runCatching {
                LocalDate.parse(historyDateKey).format(
                    DateTimeFormatter.ofPattern("MMMM d", Locale.getDefault()),
                )
            }.getOrDefault(historyDateKey)
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.completeGoal(id, studiedMinutes, studiedSeconds, scheduledDate)) {
                is Resource.Success -> {
                    _uiState.update { state ->
                        val now = java.time.Instant.now().toString()
                        val sec = if (studiedSeconds > 0) studiedSeconds else studiedMinutes * 60
                        state.copy(
                            isSavingGoal = false,
                            goals = state.goals.map {
                                if (it.id == id) it.copy(
                                    completed = true,
                                    completedAt = now,
                                    studiedMinutes = studiedMinutes,
                                    studiedSeconds = sec,
                                    status = "completed",
                                    scheduledDate = scheduledDate ?: it.scheduledDate,
                                ) else it
                            },
                            goalMessage = "Goal completed for $historyDateLabel. Open Completed and select $historyDateLabel to find it.",
                            goalAction = "complete",
                        )
                    }
                    loadStreaks()
                    loadEkagraAnalytics()
                }
                is Resource.Error   -> {
                    recordGoalOperationFailure("complete")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                    loadGoals()
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.deleteGoal(id)) {
                is Resource.Success -> _uiState.update { state ->
                    state.copy(
                        isSavingGoal = false,
                        goals = state.goals.filter { it.id != id },
                        goalMessage = "Goal deleted",
                        goalAction = "delete",
                        goalActionGoalId = id,
                    )
                }
                is Resource.Error   -> {
                    recordGoalOperationFailure("delete")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadRecentlyDeletedGoals() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDeletedGoals = true, goalError = null) }
            when (val r = homeRepository.getRecentlyDeletedGoals()) {
                is Resource.Success -> _uiState.update {
                    it.copy(isLoadingDeletedGoals = false, recentlyDeletedGoals = r.data)
                }
                is Resource.Error -> _uiState.update { it.copy(isLoadingDeletedGoals = false, goalError = r.message) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun restoreGoal(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.restoreGoal(id)) {
                is Resource.Success -> _uiState.update { state ->
                    state.copy(
                        isSavingGoal = false,
                        goals = listOf(r.data) + state.goals.filterNot { it.id == id },
                        recentlyDeletedGoals = state.recentlyDeletedGoals.filterNot { it.id == id },
                        goalMessage = "Goal restored",
                        goalAction = "restore",
                        goalActionGoalId = id,
                    )
                }
                is Resource.Error -> {
                    recordGoalOperationFailure("restore")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun reopenGoal(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.reopenGoal(id)) {
                is Resource.Success -> _uiState.update { state ->
                    state.copy(
                        isSavingGoal = false,
                        goals = state.goals.map { if (it.id == id) r.data else it },
                        goalMessage = "Goal reopened",
                        goalAction = "reopen",
                        goalActionGoalId = id,
                    )
                }
                is Resource.Error -> {
                    recordGoalOperationFailure("reopen")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun repeatGoal(id: String, scheduledDate: String) {
        viewModelScope.launch {
            when (val r = homeRepository.repeatGoal(id, scheduledDate)) {
                is Resource.Success -> _uiState.update { state ->
                    // The server dedupes: repeating something already on that day
                    // returns the existing goal rather than making a copy. Don't
                    // add it twice, and tell the truth about what happened.
                    if (r.data.alreadyExisted) {
                        state.copy(goalMessage = "Already on today's list", goalAction = "repeat")
                    } else {
                        state.copy(
                            goals = listOf(r.data) + state.goals,
                            goalMessage = "Goal repeated for today!",
                            goalAction = "repeat",
                        )
                    }
                }
                is Resource.Error -> {
                    recordGoalOperationFailure("repeat")
                    _uiState.update { it.copy(goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /** Brings the chosen goals onto today in one write. The server skips any that
     *  are already there, so double-tapping cannot duplicate the list. */
    fun repeatGoals(goalIds: List<String>) {
        if (goalIds.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            when (val r = homeRepository.repeatGoals(goalIds)) {
                is Resource.Success -> {
                    _uiState.update { it.copy(isSavingGoal = false, goalMessage = r.data.message, goalAction = "repeat") }
                    // Reload rather than prepending: the response contains only the
                    // newly created copies, and skipped ones must not appear twice.
                    loadGoals()
                }
                is Resource.Error -> {
                    recordGoalOperationFailure("repeat_bulk")
                    _uiState.update { it.copy(isSavingGoal = false, goalError = r.message) }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    /** Schedules selected goals on a specific day using the server's idempotent
     * single-goal repeat operation. Optionally converts their source goals into
     * daily-repeat goals so subsequent days continue automatically. */
    fun repeatGoalsOnDate(
        goals: List<com.safarparmar.app.domain.model.Goal>,
        scheduledDate: String,
        repeatDaily: Boolean,
    ) {
        if (goals.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingGoal = true, goalError = null) }
            var createdOrCovered = 0
            var firstError: String? = null
            for (goal in goals) {
                if (repeatDaily && goal.goalKind != "repeat") {
                    when (val update = homeRepository.updateGoalDetails(
                        id = goal.id,
                        title = goal.title,
                        description = goal.description,
                        priority = goal.priority,
                        scheduledDate = goal.scheduledDate,
                        startedAt = goal.startedAt,
                        subtasks = goal.subtasks,
                        goalKind = "repeat",
                        unitType = goal.unitType,
                        linkedFocusEnabled = goal.linkedFocusEnabled,
                        plannedFocusMinutes = goal.plannedFocusMinutes,
                        targetValue = goal.targetValue,
                        achievedValue = goal.achievedValue,
                        status = goal.status,
                        carryForwardMode = goal.carryForwardMode,
                    )) {
                        is Resource.Error -> if (firstError == null) firstError = update.message
                        else -> Unit
                    }
                }
                when (val repeated = homeRepository.repeatGoal(goal.id, scheduledDate)) {
                    is Resource.Success -> createdOrCovered += 1
                    is Resource.Error -> if (firstError == null) firstError = repeated.message
                    is Resource.Loading -> Unit
                }
            }
            _uiState.update {
                it.copy(
                    isSavingGoal = false,
                    goalMessage = if (createdOrCovered > 0) {
                        "$createdOrCovered goal${if (createdOrCovered == 1) "" else "s"} scheduled for ${IstDateUtils.labelFor(scheduledDate)}"
                    } else null,
                    goalAction = if (createdOrCovered > 0) "repeat" else null,
                    goalError = if (createdOrCovered == goals.size) null else firstError ?: "Some goals could not be repeated",
                )
            }
            loadGoals()
        }
    }

    fun clearGoalMessage() {
        _uiState.update { it.copy(goalMessage = null, goalAction = null, goalActionGoalId = null) }
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

    private fun loadMonthlyReportForMonth(month: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingReport = true, monthlyReport = null, reportError = null) }
            when (val r = homeRepository.generateMonthlyReport(month)) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingReport = false, monthlyReport = r.data, reportError = null) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingReport = false, reportError = r.message ?: "Failed to load report") }
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
