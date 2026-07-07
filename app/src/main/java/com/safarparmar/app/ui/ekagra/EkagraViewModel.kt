package com.safarparmar.app.ui.ekagra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.repository.EkagraRepository
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import kotlin.math.roundToInt

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val data: FocusStatsResponse) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

@HiltViewModel
class EkagraViewModel @Inject constructor(
    private val repo: EkagraRepository,
    private val homeRepo: HomeRepository,
    val dataStore: com.safarparmar.app.data.local.SafarDataStore,
    val focusShieldRepo: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val stats = _stats.asStateFlow()

    // Kept for screen compatibility, but active/open sessions are now local-only drafts.
    private val _openSessions = MutableStateFlow<List<EkagraSession>>(emptyList())
    val openSessions = _openSessions.asStateFlow()

    private val _activeSession = MutableStateFlow<EkagraSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _ekagraAnalytics = MutableStateFlow(EkagraAnalyticsStats())
    val ekagraAnalytics = _ekagraAnalytics.asStateFlow()

    private val _tasks = MutableStateFlow<List<Goal>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _allGoals = MutableStateFlow<List<Goal>>(emptyList())
    val allGoals = _allGoals.asStateFlow()

    private var activeSessionId: String? = null
    private var sessionStartedAt: String? = null

    val showDurationPrompt = dataStore.showEkagraDurationPrompt.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun disableDurationPrompt() {
        viewModelScope.launch {
            dataStore.setShowEkagraDurationPrompt(false)
        }
    }

    fun setAutoStartBreak(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setAutoStartBreak(enabled)
        }
    }

    init {
        // Initial fetch; periodic refresh is now driven from the screen via
        // repeatOnLifecycle so polling pauses when Ekagra is not on top.
        loadStats()
        refreshEkagra()
        loadTasks()
    }

    fun loadStats() {
        viewModelScope.launch {
            _stats.value = StatsUiState.Loading
            _stats.value = when (val r = repo.getStats()) {
                is Resource.Success -> StatsUiState.Success(r.data)
                is Resource.Error -> StatsUiState.Error(r.message ?: "Error")
                is Resource.Loading -> StatsUiState.Loading
            }
        }
    }

    fun refreshEkagra() {
        _openSessions.value = emptyList()
        loadEkagraAnalytics()
    }

    fun loadOpenSessions() {
        _openSessions.value = emptyList()
    }

    fun loadEkagraAnalytics() {
        viewModelScope.launch {
            when (val r = repo.getEkagraAnalytics()) {
                is Resource.Success -> _ekagraAnalytics.value = r.data
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            when (val r = homeRepo.getGoals()) {
                is Resource.Success -> {
                    _allGoals.value = r.data
                    _tasks.value = r.data.filter { it.source == "ekagra" }
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun onSessionStarted(
        taskText: String,
        totalSeconds: Int,
        goalId: String? = null,
        goalTitle: String? = null,
        mode: String = "Timer",
        remainingSeconds: Int = totalSeconds,
    ) {
        val now = Instant.now().toString()
        val id = "local-${System.currentTimeMillis()}"
        activeSessionId = id
        sessionStartedAt = now
        _activeSession.value = EkagraSession(
            id = id,
            goalId = goalId,
            goalTitle = goalTitle,
            sessionType = if (goalId.isNullOrBlank()) "named" else "goal",
            sessionTitle = taskText.ifBlank { goalTitle ?: "Untitled" },
            source = if (goalId.isNullOrBlank()) "manual" else "goal_continue",
            status = "active",
            mode = mode,
            totalSeconds = totalSeconds,
            remainingSeconds = remainingSeconds,
            isRunning = true,
            sessionStartedAt = now,
            createdAt = now,
            updatedAt = now,
        )
    }

    fun pauseActiveSession(totalSeconds: Int, secondsLeft: Int, mode: String, goalTitle: String? = null) {
        updateLocalDraft(totalSeconds, secondsLeft, mode, false, goalTitle)
    }

    fun resumeSession(session: EkagraSession, totalSeconds: Int, secondsLeft: Int, mode: String) {
        activeSessionId = session.id
        sessionStartedAt = session.sessionStartedAt ?: Instant.now().toString()
        _activeSession.value = session.copy(
            status = "active",
            mode = mode,
            totalSeconds = totalSeconds,
            remainingSeconds = secondsLeft,
            isRunning = true,
            sessionStartedAt = sessionStartedAt,
            updatedAt = Instant.now().toString(),
        )
    }

    fun syncActiveSession(totalSeconds: Int, secondsLeft: Int, mode: String, isRunning: Boolean, goalTitle: String? = null) {
        updateLocalDraft(totalSeconds, secondsLeft, mode, isRunning, goalTitle)
    }

    private fun updateLocalDraft(totalSeconds: Int, secondsLeft: Int, mode: String, isRunning: Boolean, goalTitle: String?) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            status = if (isRunning) "active" else "paused",
            mode = mode,
            totalSeconds = totalSeconds,
            remainingSeconds = secondsLeft.coerceIn(0, totalSeconds.coerceAtLeast(1)),
            isRunning = isRunning,
            goalTitle = goalTitle ?: current.goalTitle,
            updatedAt = Instant.now().toString(),
        )
    }

    fun addTitleToActiveSession(title: String) {
        val cleanTitle = title.trim().ifBlank { return }
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            sessionType = if (current.goalId.isNullOrBlank()) "named" else current.sessionType,
            sessionTitle = cleanTitle,
            goalTitle = current.goalTitle,
            source = if (current.goalId.isNullOrBlank()) "manual" else current.source,
            updatedAt = Instant.now().toString(),
        )
    }

    fun linkActiveSessionToGoal(goal: Goal) {
        val current = _activeSession.value ?: return
        _activeSession.value = current.copy(
            goalId = goal.id,
            goalTitle = goal.title,
            sessionType = "goal",
            source = "goal_continue",
            importedFromGoal = goal.importedFromGoal,
            updatedAt = Instant.now().toString(),
        )
    }

    fun createGoalAndCompleteSession(sessionId: String, title: String, totalSeconds: Int, secondsLeft: Int, mode: String, startedAt: String?) {
        val cleanTitle = title.trim().ifBlank { return }
        viewModelScope.launch {
            val today = IstDateUtils.todayKey()
            val nowIso = LocalDateTime.now(ZoneOffset.ofHoursMinutes(5, 30))
                .toInstant(ZoneOffset.ofHoursMinutes(5, 30)).toString()
            when (val created = homeRepo.addGoal(
                title = cleanTitle,
                description = null,
                priority = "medium",
                scheduledDate = today,
                startedAt = nowIso,
                subtasks = emptyList(),
                source = "manual",
                linkedFocusEnabled = false,
                unitType = "binary",
                plannedFocusMinutes = null,
            )) {
                is Resource.Success -> completeSession(
                    sessionId = sessionId,
                    totalSeconds = totalSeconds,
                    secondsLeft = secondsLeft,
                    mode = mode,
                    startedAt = startedAt,
                    taskTitle = cleanTitle,
                    goalId = created.data.id,
                    goalTitle = created.data.title,
                )
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun linkGoalAndCompleteSession(
        sessionId: String,
        goal: Goal,
        totalSeconds: Int,
        secondsLeft: Int,
        mode: String,
        startedAt: String?,
        markGoalComplete: Boolean = false,
        endedAt: String? = null,
    ) {
        completeSession(
            sessionId = sessionId,
            totalSeconds = totalSeconds,
            secondsLeft = secondsLeft,
            mode = mode,
            startedAt = startedAt,
            goalId = goal.id,
            goalTitle = goal.title,
            markGoalComplete = markGoalComplete,
            endedAt = endedAt
        )
    }

    fun onSessionCompleted(totalSeconds: Int, secondsLeft: Int, mode: String = "Timer") {
        val id = activeSessionId ?: _activeSession.value?.id ?: return
        completeSession(id, totalSeconds, secondsLeft, mode, sessionStartedAt)
    }

    fun onSessionStopped(totalSeconds: Int, secondsLeft: Int) {
        pauseActiveSession(totalSeconds, secondsLeft, "Timer")
    }

    fun completeSession(
        sessionId: String,
        totalSeconds: Int,
        secondsLeft: Int,
        mode: String = "Timer",
        startedAt: String? = null,
        taskTitle: String? = null,
        goalId: String? = null,
        goalTitle: String? = null,
        markGoalComplete: Boolean = false,
        endedAt: String? = null,
    ) {
        val current = _activeSession.value
        val actualMinutes = if (mode == "stopwatch") {
            (secondsLeft / 60.0).roundToInt()
        } else {
            ((totalSeconds - secondsLeft) / 60.0).roundToInt()
        }
        val plannedMinutes = if (mode == "stopwatch") 0 else (totalSeconds / 60.0).roundToInt()
        val started = startedAt ?: current?.sessionStartedAt ?: sessionStartedAt ?: if (mode == "stopwatch") {
            Instant.now().minusSeconds(secondsLeft.toLong()).toString()
        } else {
            Instant.now().minusSeconds((totalSeconds - secondsLeft).toLong()).toString()
        }
        val cleanGoalId = goalId ?: current?.goalId
        val cleanGoalTitle = goalTitle ?: current?.goalTitle
        val cleanTitle = taskTitle?.trim()?.takeIf { it.isNotBlank() && it != "Untitled" }
            ?: current?.sessionTitle?.trim()?.takeIf { it.isNotBlank() && it != "Untitled" }
            ?: cleanGoalTitle
            ?: "Untitled"
        val shieldWasActive = focusShieldRepo.sessionActive.value || focusShieldRepo.isEnabled.value

        viewModelScope.launch {
            // Save the new record before touching the old one — if the save fails (no
            // network, process death), we must not have already deleted the original.
            val saveResult = repo.saveSession(
                clientSessionId = sessionId.takeIf { it.startsWith("local-") },
                mode = mode,
                startedAt = started,
                endedAt = endedAt ?: Instant.now().toString(),
                plannedDurationMinutes = plannedMinutes,
                actualDurationMinutes = actualMinutes,
                goalId = cleanGoalId?.takeIf { it.isNotBlank() && !it.startsWith("named:") },
                goalTitle = cleanGoalTitle,
                taskTitle = cleanTitle,
                markGoalComplete = markGoalComplete,
                shieldEnabled = shieldWasActive,
            )
            focusShieldRepo.deactivateSession()
            when (saveResult) {
                is Resource.Success -> {
                    if (!sessionId.startsWith("local-")) {
                        repo.deleteSession(sessionId)
                    }
                    if (activeSessionId == sessionId || current?.id == sessionId) clearLocalDraft()
                    loadStats()
                    refreshEkagra()
                    loadTasks()
                }
                is Resource.Error, is Resource.Loading -> {
                    // Save failed — keep the local draft intact instead of discarding this
                    // session's data, so the user can retry ending it later.
                }
            }
        }
    }

    fun discardSession(sessionId: String) {
        if (activeSessionId == sessionId || _activeSession.value?.id == sessionId) {
            clearLocalDraft()
        }
    }

    fun deleteSession(sessionId: String) {
        discardSession(sessionId)
    }

    fun clearLocalDraft() {
        activeSessionId = null
        sessionStartedAt = null
        _activeSession.value = null
        _openSessions.value = emptyList()
    }

    fun createTaskAsGoal(taskText: String) {
        viewModelScope.launch {
            val today = IstDateUtils.todayKey()
            val nowIso = LocalDateTime.now(ZoneOffset.ofHoursMinutes(5, 30))
                .toInstant(ZoneOffset.ofHoursMinutes(5, 30)).toString()
            homeRepo.addGoal(
                title = taskText,
                description = null,
                priority = "medium",
                scheduledDate = today,
                startedAt = nowIso,
                subtasks = emptyList(),
                source = "manual",
                linkedFocusEnabled = false,
            )
            loadTasks()
        }
    }

    fun completeTask(task: Goal) {
        viewModelScope.launch {
            homeRepo.completeGoal(task.id, studiedMinutes = 0)
            loadTasks()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            homeRepo.deleteGoal(taskId)
            loadTasks()
        }
    }
}
