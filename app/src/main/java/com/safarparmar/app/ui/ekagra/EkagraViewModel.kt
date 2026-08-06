package com.safarparmar.app.ui.ekagra

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.Goal
import com.safarparmar.app.domain.model.GoalLinkedSession
import com.safarparmar.app.domain.repository.EkagraRepository
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.data.remote.api.TopicPatchRequest
import com.safarparmar.app.domain.model.studyplanner.TopicStatus
import com.safarparmar.app.util.IstDateUtils
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val appContext: Context,
    private val repo: EkagraRepository,
    private val homeRepo: HomeRepository,
    private val plannerRepo: StudyPlannerRepository,
    val dataStore: com.safarparmar.app.data.local.SafarDataStore,
    val focusShieldRepo: com.safarparmar.app.ui.ekagra.focusshield.FocusShieldRepository,
) : ViewModel() {

    sealed interface StudyTimeSaveResult {
        data object Saved : StudyTimeSaveResult
        data object SavedOnPhone : StudyTimeSaveResult
    }

    sealed interface TopicDoneResult {
        data object Done : TopicDoneResult
        data class Error(val message: String) : TopicDoneResult
    }

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

    private val _linkedSessions = MutableStateFlow<List<GoalLinkedSession>>(emptyList())
    val linkedSessions = _linkedSessions.asStateFlow()

    fun loadLinkedSessions() {
        viewModelScope.launch {
            when (val r = repo.getLinkedSessions()) {
                is Resource.Success -> _linkedSessions.value = r.data
                is Resource.Error, is Resource.Loading -> Unit
            }
        }
    }

    private val _topicLinkedSessions = MutableStateFlow<List<com.safarparmar.app.domain.model.TopicLinkedSession>>(emptyList())
    val topicLinkedSessions = _topicLinkedSessions.asStateFlow()

    fun loadTopicLinkedSessions(planId: String? = null) {
        viewModelScope.launch {
            when (val r = repo.getTopicLinkedSessions(planId)) {
                is Resource.Success -> _topicLinkedSessions.value = r.data
                is Resource.Error, is Resource.Loading -> Unit
            }
        }
    }

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

    fun setTimerAlertStyle(style: com.safarparmar.app.data.local.TimerAlertStyle) {
        viewModelScope.launch {
            dataStore.setTimerAlertStyle(style)
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

    fun linkGoalAndSaveSession(
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

    /**
     * Renames / re-associates an EXISTING history session in place (keeps its id).
     *
     * This replaces the old "completeSession → save a new row + delete the old one"
     * approach for editing a session that is already saved. That copy-and-delete
     * minted a new id every edit and produced a duplicate whenever the delete half
     * didn't land — the "renamed session clones itself and rotates in pairs" bug.
     * A real in-place update can't duplicate because the row keeps its identity.
     */
    fun updateExistingSession(
        sessionId: String,
        taskTitle: String? = null,
        goalId: String? = null,
        goalTitle: String? = null,
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
        markGoalComplete: Boolean = false,
        markTopicDone: Boolean = false,
    ) {
        viewModelScope.launch {
            when (repo.updateSession(sessionId, taskTitle, goalId, goalTitle, topicId, planId, topicTitle)) {
                is Resource.Success -> {
                    if (markGoalComplete && !goalId.isNullOrBlank()) {
                        homeRepo.completeGoal(goalId, studiedMinutes = 0)
                    }
                    if (markTopicDone && !topicId.isNullOrBlank() && !planId.isNullOrBlank()) {
                        plannerRepo.updateTopic(
                            planId,
                            topicId,
                            TopicPatchRequest(status = TopicStatus.DONE, clientDateKey = IstDateUtils.todayKey()),
                        )
                        com.safarparmar.app.ui.studyplanner.PlannerTopicEventBus.postTopicCompleted(planId)
                    }
                    loadStats()
                    refreshEkagra()
                    loadTasks()
                }
                is Resource.Error, is Resource.Loading -> Unit
            }
        }
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
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
        markTopicDone: Boolean = false,
    ) {
        // A non-local id identifies a history row that has already been persisted.
        // Organizing it must never mint a replacement row: save-then-delete is not
        // atomic and a failed delete leaves duplicate history entries.
        if (!sessionId.startsWith("local-")) {
            updateExistingSession(
                sessionId = sessionId,
                taskTitle = taskTitle,
                goalId = goalId,
                goalTitle = goalTitle,
                topicId = topicId,
                planId = planId,
                topicTitle = topicTitle,
                markGoalComplete = markGoalComplete,
                markTopicDone = markTopicDone,
            )
            return
        }

        val current = _activeSession.value
        val actualSeconds = if (mode == "stopwatch") secondsLeft else (totalSeconds - secondsLeft)

        // Never persist a zero-length session.
        //
        // actualSeconds is derived as (totalSeconds - secondsLeft), so it is 0 whenever
        // this runs while the timer is still full — most notably the instant an
        // auto-break starts (break is at full length, hasn't ticked yet). Without this
        // guard we POST a `completed: true` session of 0 minutes, which the user reads
        // as "my session didn't save" and which pollutes history and analytics.
        //
        // TimerService's own save path already had this guard (`if (actual == 0) return`);
        // this path did not, which is why the corrupt rows all came from here.
        if (actualSeconds <= 0) return

        // Kept for the backend's older minute-level fields and for planned-vs-actual
        // fallbacks — actualSeconds is the precise value now used for aggregation.
        val actualMinutes = (actualSeconds / 60.0).roundToInt()
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
            ?: topicTitle
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
                actualDurationSeconds = actualSeconds,
                goalId = cleanGoalId?.takeIf { it.isNotBlank() && !it.startsWith("named:") },
                goalTitle = cleanGoalTitle,
                topicId = topicId,
                planId = planId,
                topicTitle = topicTitle,
                markTopicDone = markTopicDone,
                taskTitle = cleanTitle,
                markGoalComplete = markGoalComplete,
                shieldEnabled = shieldWasActive,
            )
            focusShieldRepo.deactivateSession()
            when (saveResult) {
                is Resource.Success -> {
                    if (activeSessionId == sessionId || current?.id == sessionId) clearLocalDraft()
                    // Tell the Study Planner (if open in another ViewModel) to reload
                    // this plan so the just-completed topic flips to done live, rather
                    // than only on the planner's next cold load.
                    if (markTopicDone && !topicId.isNullOrBlank() && !planId.isNullOrBlank()) {
                        com.safarparmar.app.ui.studyplanner.PlannerTopicEventBus.postTopicCompleted(planId)
                    }
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

    /**
     * Safely records a planner-topic study session before asking whether the
     * topic itself is finished. The local queue is written first, so closing
     * the app or losing internet cannot erase the student's study time.
     */
    internal fun saveTopicStudyTime(
        pending: PendingEndedEkagraSession,
        onResult: (StudyTimeSaveResult) -> Unit,
    ) {
        val actualSeconds = topicStudyActualSeconds(pending)
        if (actualSeconds <= 0) return

        val clientSessionId = pending.sessionId
        val startedAt = pending.startedAt
            ?: Instant.now().minusSeconds(actualSeconds.toLong()).toString()
        val endedAt = pending.endedAt ?: Instant.now().toString()
        val plannedMinutes = if (pending.mode.equals("stopwatch", ignoreCase = true)) {
            0
        } else {
            (pending.totalSeconds / 60.0).roundToInt()
        }
        val shieldWasActive = focusShieldRepo.sessionActive.value || focusShieldRepo.isEnabled.value

        EkagraPendingSessionSaveStore.enqueue(
            appContext,
            PendingEkagraSessionSave(
                clientSessionId = clientSessionId,
                mode = pending.mode,
                startedAt = startedAt,
                endedAt = endedAt,
                plannedDurationMinutes = plannedMinutes,
                actualDurationMinutes = (actualSeconds / 60.0).roundToInt(),
                actualDurationSeconds = actualSeconds,
                goalId = null,
                goalTitle = null,
                topicId = pending.topicId,
                planId = pending.planId,
                topicTitle = pending.topicTitle,
                taskTitle = pending.topicTitle ?: "Study from Exam Planner",
                shieldEnabled = shieldWasActive,
            ),
        )
        EkagraSessionSaveWorker.enqueue(appContext)

        viewModelScope.launch {
            val uploaded = EkagraSessionSaveWorker.drainPendingSaves(appContext)
            if (activeSessionId == clientSessionId || _activeSession.value?.id == clientSessionId) {
                clearLocalDraft()
            }
            focusShieldRepo.deactivateSession()
            loadStats()
            refreshEkagra()
            onResult(if (uploaded) StudyTimeSaveResult.Saved else StudyTimeSaveResult.SavedOnPhone)
        }
    }

    fun markPlannerTopicDone(
        planId: String,
        topicId: String,
        onResult: (TopicDoneResult) -> Unit,
    ) {
        viewModelScope.launch {
            when (
                val result = plannerRepo.updateTopic(
                    planId,
                    topicId,
                    TopicPatchRequest(
                        status = TopicStatus.DONE,
                        clientDateKey = IstDateUtils.todayKey(),
                    ),
                )
            ) {
                is Resource.Success -> {
                    com.safarparmar.app.ui.studyplanner.PlannerTopicEventBus.postTopicCompleted(planId)
                    onResult(TopicDoneResult.Done)
                }
                is Resource.Error -> onResult(
                    TopicDoneResult.Error(
                        if (result.message.isBlank()) "Couldn't mark this topic as done." else result.message,
                    ),
                )
                is Resource.Loading -> Unit
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
