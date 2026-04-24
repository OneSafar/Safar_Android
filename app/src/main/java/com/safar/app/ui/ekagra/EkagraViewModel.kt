package com.safar.app.ui.ekagra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.domain.repository.EkagraRepository
import com.safar.app.domain.repository.HomeRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
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
) : ViewModel() {

    private val _stats = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val stats = _stats.asStateFlow()

    private var activeSessionId: String? = null
    private var sessionStartedAt: String? = null

    init {
        loadStats()
        startAutoRefresh()
    }


    fun loadStats() {
        viewModelScope.launch {
            _stats.value = StatsUiState.Loading
            try {
                when (val r = repo.getStats()) {
                    is Resource.Success -> _stats.value = StatsUiState.Success(r.data)
                    is Resource.Error   -> _stats.value = StatsUiState.Error(r.message ?: "Error")
                    is Resource.Loading -> Unit
                }
            } catch (e: Exception) {
                _stats.value = StatsUiState.Error("No internet connection")
            }
        }
    }

    private fun startAutoRefresh() {
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                try {
                    when (val r = repo.getStats()) {
                        is Resource.Success -> _stats.value = StatsUiState.Success(r.data)
                        is Resource.Error   -> _stats.value = StatsUiState.Error(r.message ?: "Error")
                        is Resource.Loading -> Unit
                    }
                } catch (e: Exception) {
                    _stats.value = StatsUiState.Error("No internet")
                }
            }
        }
    }


    /**
     * Called when the user hits Start.
     * Creates (activates) a session on the backend and stores the session id.
     *
     * [taskText]     — the task the user typed, or blank for a plain focus session.
     * [totalSeconds] — planned duration in seconds.
     */
    fun onSessionStarted(taskText: String, totalSeconds: Int) {
        val now = Instant.now().toString()
        sessionStartedAt = now
        viewModelScope.launch {
            when (val r = repo.activateSession(
                title         = taskText.ifBlank { "Focus Session" },
                totalSeconds  = totalSeconds,
                sessionStartedAt = now,
            )) {
                is Resource.Success -> activeSessionId = r.data.id
                is Resource.Error   -> { /* non-fatal — timer still runs locally */ }
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * Called when the timer reaches 0 (natural completion).
     *
     * [totalSeconds]   — planned duration.
     * [secondsLeft]    — should be 0 on natural completion, but pass it anyway.
     */
    fun onSessionCompleted(totalSeconds: Int, secondsLeft: Int) {
        val id = activeSessionId ?: return
        val elapsedSeconds = totalSeconds - secondsLeft
        viewModelScope.launch {
            repo.completeSession(
                sessionId        = id,
                totalSeconds     = totalSeconds,
                elapsedSeconds   = elapsedSeconds,
                remainingSeconds = secondsLeft,
                sessionStartedAt = sessionStartedAt,
            )
            activeSessionId  = null
            sessionStartedAt = null
            loadStats()
        }
    }

    /**
     * Called when the user manually stops / resets the timer early.
     * We still complete the session on the backend with however many seconds elapsed,
     * so partial time is recorded in analytics.
     */
    fun onSessionStopped(totalSeconds: Int, secondsLeft: Int) {
        onSessionCompleted(totalSeconds, secondsLeft)
    }


    fun createTaskAsGoal(taskText: String) {
        viewModelScope.launch {
            val today  = LocalDate.now().toString()
            val nowIso = LocalDateTime.now(ZoneOffset.ofHoursMinutes(5, 30))
                .toInstant(ZoneOffset.ofHoursMinutes(5, 30)).toString()
            homeRepo.addGoal(taskText, null, "medium", today, nowIso, emptyList())
        }
    }
}
