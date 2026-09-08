package com.safarparmar.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.assignedDateKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import android.content.Context
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.notifications.SafarNotificationChannels
import com.safarparmar.app.ui.achievements.AchievementImages
import kotlinx.coroutines.CoroutineExceptionHandler

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val studyPlannerRepository: StudyPlannerRepository,
    private val dataStore: SafarDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e("DashboardViewModel", "Dashboard load failed", throwable)
        FirebaseCrashlytics.getInstance().recordException(throwable)
        _uiState.update {
            it.copy(
                isLoading = false,
                error = throwable.localizedMessage ?: "Dashboard could not load. Pull to refresh.",
            )
        }
    }

    private var loadJob: Job? = null

    init { loadAll() }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh    -> { homeRepository.invalidateReadSnapshots(); loadAll() }
            is DashboardEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    fun dismissWelcome() {
        _uiState.update { it.copy(showWelcomeOverlay = false) }
        viewModelScope.launch { dataStore.setWelcomeSeen(true) }
    }

    fun selectAchievement(achievementId: String?) {
        viewModelScope.launch(exceptionHandler) {
            when (val result = homeRepository.selectAchievement(achievementId)) {
                is Resource.Success -> {
                    val selId = result.data.selectedId
                    _uiState.update {
                        it.copy(
                            activeTitle = result.data.title,
                            activeTitleId = selId,
                            activeTitleImageUrl = selId.takeIf { id -> id.isNotEmpty() }
                                ?.let { id -> AchievementImages.urlFor(id) }
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message ?: "Failed to set active achievement") }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadAll() {
        // Do not restart an in-flight refresh or cancel achievement delivery.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch(exceptionHandler) {
            _uiState.update { it.copy(isLoading = true, error = null, sectionErrors = emptyMap()) }
            try {
                supervisorScope {
                    launch {
                        val userName = dataStore.userName.first().orEmpty()
                        val avatar = dataStore.userAvatar.first()
                        val welcomeSeen = dataStore.isWelcomeSeen.first()
                        _uiState.update { it.copy(userName = userName, userAvatar = avatar, profileReady = true, showWelcomeOverlay = !welcomeSeen) }
                    }
                    launch { loadSection(DashboardSection.STREAKS, { homeRepository.getStreaks() }) { state, data -> state.copy(streaks = data) } }
                    launch { loadSection(DashboardSection.GOALS, { homeRepository.getGoals() }) { state, goals ->
                        val today = com.safarparmar.app.util.IstDateUtils.todayKey()
                        state.copy(todayGoals = goals.filter { it.source != "ekagra" && it.assignedDateKey() == today }, completedGoals = goals.filter { it.completed }.takeLast(5))
                    } }
                    launch { loadSection(DashboardSection.MOODS, { homeRepository.getMoods() }) { state, moods ->
                        val today = com.safarparmar.app.util.IstDateUtils.todayKey()
                        val date = LocalDate.now()
                        val monday = date.minusDays((date.dayOfWeek.value - 1).toLong())
                        state.copy(todayMood = moods.firstOrNull { it.timestamp.startsWith(today) }, weeklyMoods = (0..6).map { index ->
                            val key = monday.plusDays(index.toLong()).toString()
                            moods.firstOrNull { it.timestamp.startsWith(key) } ?: Mood(intensity = 0, mood = "", timestamp = key)
                        })
                    } }
                    launch { loadSection(DashboardSection.REPORT, { homeRepository.getMonthlyReport() }) { state, data -> state.copy(monthlyReport = data) } }
                    launch { loadSection(DashboardSection.TITLE, { homeRepository.getActiveTitle() }) { state, title ->
                        state.copy(activeTitle = title.title, activeTitleId = title.selectedId, activeTitleImageUrl = title.selectedId.takeIf(String::isNotEmpty)?.let(AchievementImages::urlFor))
                    } }
                    launch {
                        loadSection(DashboardSection.ACHIEVEMENTS, { homeRepository.getAchievements() }) { state, data ->
                            state.copy(allAchievements = data, earnedAchievements = data.filter { it.earned })
                        }
                        if (DashboardSection.ACHIEVEMENTS !in _uiState.value.sectionErrors) deliverAchievements()
                    }
                    launch { loadSection(DashboardSection.HISTORY, { homeRepository.getLoginHistory() }) { state, data -> state.copy(loginHistory = data) } }
                    launch {
                        loadSection(DashboardSection.PLAN, {
                            val plan = loadStudyPlanCard()
                            if (plan.errorMessage != null && plan.planId == null) Resource.Error(plan.errorMessage) else Resource.Success(plan)
                        }) { state, data -> state.copy(studyPlan = data) }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false, profileReady = true) }
            }
        }
    }

    private suspend fun <T> loadSection(
        section: DashboardSection,
        request: suspend () -> Resource<T>,
        apply: (DashboardUiState, T) -> DashboardUiState,
    ) {
        try {
            when (val result = request()) {
                is Resource.Success -> _uiState.update { state ->
                    apply(state, result.data).copy(loadedSections = state.loadedSections + section, sectionErrors = state.sectionErrors - section)
                }
                is Resource.Error -> _uiState.update { it.copy(sectionErrors = it.sectionErrors + (section to result.message)) }
                is Resource.Loading -> Unit
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            _uiState.update { it.copy(sectionErrors = it.sectionErrors + (section to (exception.localizedMessage ?: "Could not load this section."))) }
        }
    }

    private suspend fun deliverAchievements() {
        val notified = dataStore.notifiedAchievements.first()
        val newlyEarned = _uiState.value.allAchievements.filter { it.earned && it.id !in notified }
        if (newlyEarned.isEmpty()) return
        _uiState.update { it.copy(celebrationAchievements = newlyEarned) }
        val enabled = dataStore.notificationsEnabled.first() && dataStore.achievementsEnabled.first()
        for (achievement in newlyEarned) {
            if (enabled) SafarNotificationManager(context).show(
                title = "Achievement Unlocked! 🏆",
                body = "You unlocked: ${achievement.name}",
                channelId = SafarNotificationChannels.ACHIEVEMENTS,
                deepLink = "safar://achievements",
                personalize = true,
            )
            dataStore.addNotifiedAchievement(achievement.id)
        }
    }

    private suspend fun loadStudyPlanCard(): DashboardStudyPlanState {
        return try {
            coroutineScope {
            val activePlanId = dataStore.plannerActivePlanId().first()
            when (val plansResult = studyPlannerRepository.listPlans()) {
                is Resource.Success -> {
                    val summaryPlan = resolveDashboardStudyPlan(plansResult.data, activePlanId)
                    if (summaryPlan == null) {
                        DashboardStudyPlanState()
                    } else {
                        // listPlans() returns summaries without the topic tree, so rollup()
                        // on them yields "0 of 0". Hydrate the full plan (like the planner's
                        // openPlan does) before computing progress; fall back to the summary
                        // if the detail fetch fails so the card still renders.
                        // getPlan can roll overdue topics forward on the server. Read the
                        // calendar afterwards so it reflects that rollover.
                        val activePlan = (studyPlannerRepository.getPlan(summaryPlan.id) as? Resource.Success)?.data ?: summaryPlan
                        val calendarResult = studyPlannerRepository.getCalendar(summaryPlan.id)
                        val calendar = when (calendarResult) {
                            is Resource.Success -> calendarResult.data
                            is Resource.Error -> emptyMap()
                            is Resource.Loading -> emptyMap()
                        }
                        val calendarError = (calendarResult as? Resource.Error)?.message
                        withContext(Dispatchers.Default) { buildDashboardStudyPlanState(activePlan, calendar, errorMessage = calendarError) }
                    }
                }
                is Resource.Error -> DashboardStudyPlanState(errorMessage = plansResult.message)
                is Resource.Loading -> DashboardStudyPlanState()
            }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DashboardStudyPlanState(errorMessage = e.localizedMessage ?: "Could not load exam planner.")
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(celebrationAchievements = emptyList()) }
    }
}
