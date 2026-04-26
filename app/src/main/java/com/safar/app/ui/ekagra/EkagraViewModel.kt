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

    private val _openSessions = MutableStateFlow<List<EkagraSession>>(emptyList())
    val openSessions = _openSessions.asStateFlow()

    private val _activeSession = MutableStateFlow<EkagraSession?>(null)
    val activeSession = _activeSession.asStateFlow()

    private val _ekagraAnalytics = MutableStateFlow(EkagraAnalyticsStats())
    val ekagraAnalytics = _ekagraAnalytics.asStateFlow()

    private val _tasks = MutableStateFlow<List<Goal>>(emptyList())
    val tasks = _tasks.asStateFlow()

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
        loadOpenSessions()
        loadEkagraAnalytics()
    }

    fun loadOpenSessions() {
        viewModelScope.launch {
            when (val sessions = repo.getOpenSessions()) {
                is Resource.Success -> _openSessions.value = sessions.data
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
            when (val active = repo.getActiveSession()) {
                is Resource.Success -> {
                    _activeSession.value = active.data
                    activeSessionId = active.data?.id ?: activeSessionId
                    sessionStartedAt = active.data?.sessionStartedAt ?: sessionStartedAt
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
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
                is Resource.Success -> _tasks.value = r.data.filter { it.source == "ekagra" }
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
        sessionStartedAt = now
        viewModelScope.launch {
            when (val r = repo.activateSession(
                title = taskText.ifBlank { goalTitle ?: "Focus Session" },
                totalSeconds = totalSeconds,
                sessionStartedAt = now,
                goalId = goalId,
                goalTitle = goalTitle,
                mode = mode,
                remainingSeconds = remainingSeconds,
            )) {
                is Resource.Success -> {
                    activeSessionId = r.data.id
                    _activeSession.value = r.data
                    loadOpenSessions()
                }
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
        }
    }

    fun pauseActiveSession(totalSeconds: Int, secondsLeft: Int, mode: String, goalTitle: String? = null) {
        val id = activeSessionId ?: _activeSession.value?.id ?: return
        viewModelScope.launch {
            repo.updateSession(
                sessionId = id,
                status = "paused",
                mode = mode,
                totalSeconds = totalSeconds,
                remainingSeconds = secondsLeft.coerceAtLeast(1),
                isRunning = false,
                sessionStartedAt = sessionStartedAt,
                goalTitle = goalTitle,
            )
            loadOpenSessions()
        }
    }

    fun resumeSession(session: EkagraSession, totalSeconds: Int, secondsLeft: Int, mode: String) {
        activeSessionId = session.id
        sessionStartedAt = session.sessionStartedAt ?: Instant.now().toString()
        viewModelScope.launch {
            when (val r = repo.updateSession(
                sessionId = session.id,
                status = "active",
                mode = mode,
                totalSeconds = totalSeconds,
                remainingSeconds = secondsLeft,
                isRunning = true,
                sessionStartedAt = sessionStartedAt,
                goalTitle = session.goalTitle ?: session.sessionTitle,
                source = session.source,
                importedFromGoal = session.importedFromGoal,
            )) {
                is Resource.Success -> _activeSession.value = r.data
                is Resource.Error -> Unit
                is Resource.Loading -> Unit
            }
            loadOpenSessions()
        }
    }

    fun syncActiveSession(totalSeconds: Int, secondsLeft: Int, mode: String, isRunning: Boolean, goalTitle: String? = null) {
        val id = activeSessionId ?: _activeSession.value?.id ?: return
        viewModelScope.launch {
            repo.updateSession(
                sessionId = id,
                status = if (isRunning) "active" else "paused",
                mode = mode,
                totalSeconds = totalSeconds,
                remainingSeconds = secondsLeft.coerceAtLeast(if (isRunning) 0 else 1),
                isRunning = isRunning,
                sessionStartedAt = sessionStartedAt,
                goalTitle = goalTitle,
            )
        }
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
    ) {
        viewModelScope.launch {
            repo.completeSession(
                sessionId = sessionId,
                totalSeconds = totalSeconds,
                elapsedSeconds = (totalSeconds - secondsLeft).coerceAtLeast(0),
                remainingSeconds = secondsLeft,
                sessionStartedAt = startedAt ?: sessionStartedAt,
                mode = mode,
            )
            if (activeSessionId == sessionId) {
                activeSessionId = null
                sessionStartedAt = null
                _activeSession.value = null
            }
            focusShieldRepo.deactivateSession()
            loadStats()
            refreshEkagra()
            loadTasks()
        }
    }

    fun discardSession(sessionId: String) {
        viewModelScope.launch {
            repo.discardSession(sessionId)
            if (activeSessionId == sessionId) {
                activeSessionId = null
                _activeSession.value = null
            }
            loadOpenSessions()
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repo.deleteSession(sessionId)
            if (activeSessionId == sessionId) {
                activeSessionId = null
                _activeSession.value = null
            }
            loadOpenSessions()
        }
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
                source = "ekagra",
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
