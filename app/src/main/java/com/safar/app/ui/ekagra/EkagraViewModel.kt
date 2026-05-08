package com.safar.app.ui.ekagra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.domain.model.EkagraAnalyticsStats
import com.safar.app.domain.model.Goal
import com.safar.app.domain.repository.EkagraRepository
import com.safar.app.domain.repository.HomeRepository
import com.safar.app.util.IstDateUtils
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

sealed class StatsUiState {
    object Loading : StatsUiState()
    data class Success(val data: FocusStatsResponse) : StatsUiState()
    data class Error(val message: String) : StatsUiState()
}

@HiltViewModel
class EkagraViewModel @Inject constructor(
    private val repo: EkagraRepository,
    private val homeRepo: HomeRepository,
    val dataStore: com.safar.app.data.local.SafarDataStore,
    val focusShieldRepo: com.safar.app.ui.ekagra.focusshield.FocusShieldRepository,
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

    init {
        loadStats()
        refreshEkagra()
        loadTasks()
        startAutoRefresh()
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

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                loadStats()
                loadEkagraAnalytics()
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
            sessionTitle = taskText.ifBlank { goalTitle ?: "Free Focus" },
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
    ) {
        val current = _activeSession.value
        val elapsedSeconds = (totalSeconds - secondsLeft).coerceAtLeast(0)
        val actualMinutes = (elapsedSeconds.coerceAtLeast(60) + 59) / 60
        val plannedMinutes = (totalSeconds.coerceAtLeast(60) + 59) / 60
        val started = startedAt ?: current?.sessionStartedAt ?: sessionStartedAt ?: Instant.now().minusSeconds(elapsedSeconds.toLong()).toString()
        val cleanGoalId = goalId ?: current?.goalId
        val cleanGoalTitle = goalTitle ?: current?.goalTitle
        val cleanTitle = taskTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: current?.sessionTitle?.trim()?.takeIf { it.isNotBlank() }
            ?: cleanGoalTitle
            ?: "Free Focus"

        viewModelScope.launch {
            repo.saveSession(
                mode = mode,
                startedAt = started,
                endedAt = Instant.now().toString(),
                plannedDurationMinutes = plannedMinutes,
                actualDurationMinutes = actualMinutes,
                goalId = cleanGoalId?.takeIf { it.isNotBlank() && !it.startsWith("named:") },
                goalTitle = cleanGoalTitle,
                taskTitle = cleanTitle,
                markGoalComplete = markGoalComplete,
            )
            if (activeSessionId == sessionId || current?.id == sessionId) clearLocalDraft()
            focusShieldRepo.deactivateSession()
            loadStats()
            refreshEkagra()
            loadTasks()
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
