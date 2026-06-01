package com.safarparmar.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.domain.repository.HomeRepository
import com.safarparmar.app.domain.repository.StudyPlannerRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.notifications.SafarNotificationChannels

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val studyPlannerRepository: StudyPlannerRepository,
    private val dataStore: SafarDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init { loadAll() }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.Refresh    -> loadAll()
            is DashboardEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    fun dismissWelcome() {
        _uiState.update { it.copy(showWelcomeOverlay = false) }
        viewModelScope.launch { dataStore.setWelcomeSeen(true) }
    }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val streaksD      = async { homeRepository.getStreaks() }
            val moodsD        = async { homeRepository.getMoods() }
            val goalsD        = async { homeRepository.getGoals() }
            val reportD       = async { homeRepository.getMonthlyReport() }
            val titleD        = async { homeRepository.getActiveTitle() }
            val achievementsD = async { homeRepository.getAchievements() }
            val historyD      = async { homeRepository.getLoginHistory() }
            val studyPlanD    = async { loadStudyPlanCard() }

            val userName      = runCatching { dataStore.userName.first() }.getOrDefault("")
            val userAvatar    = runCatching { dataStore.userAvatar.first() }.getOrDefault(null)
            val welcomeSeen   = runCatching { dataStore.isWelcomeSeen.first() }.getOrDefault(false)

            val streaks      = (streaksD.await()      as? Resource.Success)?.data ?: Streaks()
            val moods        = (moodsD.await()        as? Resource.Success)?.data ?: emptyList()
            val goals        = (goalsD.await()        as? Resource.Success)?.data ?: emptyList()
            val report       = (reportD.await()       as? Resource.Success)?.data
            val title        = (titleD.await()        as? Resource.Success)?.data
            val achievements = (achievementsD.await() as? Resource.Success)?.data ?: emptyList()
            val loginHistory = (historyD.await()      as? Resource.Success)?.data ?: emptyList()
            val studyPlan    = studyPlanD.await()

            val today          = LocalDate.now().toString()          // "2026-03-29"
            val todayGoals     = goals.filter { it.scheduledDate?.startsWith(today) == true }
            val completedGoals = goals.filter { it.completed }.takeLast(5)
            val todayMood      = moods.firstOrNull { it.timestamp.startsWith(today) }
            val weeklyMoods    = moods.take(7)

            // Trigger local notifications for newly earned achievements
            val notifiedAchievements = dataStore.notifiedAchievements.first()
            val notificationsEnabled = dataStore.notificationsEnabled.first() && dataStore.achievementsEnabled.first()
            val newlyEarned = achievements.filter { it.earned && !notifiedAchievements.contains(it.id) }
            
            if (newlyEarned.isNotEmpty() && notificationsEnabled) {
                val notificationManager = SafarNotificationManager(context)
                newlyEarned.forEach { achievement ->
                    notificationManager.show(
                        title = "Achievement Unlocked! \uD83C\uDFC6",
                        body = "You unlocked: ${achievement.name}",
                        channelId = SafarNotificationChannels.ACHIEVEMENTS,
                        deepLink = "safar://achievements"
                    )
                    dataStore.addNotifiedAchievement(achievement.id)
                }
            }

            _uiState.update {
                it.copy(
                    isLoading          = false,
                    userName           = userName ?: "",
                    userAvatar         = userAvatar,
                    activeTitle        = title?.title ?: "",
                    activeTitleId      = title?.selectedId ?: "",
                    streaks            = streaks,
                    todayMood          = todayMood,
                    todayGoals         = todayGoals,
                    completedGoals     = completedGoals,
                    monthlyReport      = report,
                    weeklyMoods        = weeklyMoods,
                    earnedAchievements = achievements.filter { a -> a.earned },
                    allAchievements    = achievements,
                    loginHistory          = loginHistory,
                    studyPlan             = studyPlan,
                    showWelcomeOverlay    = !welcomeSeen
                )
            }
        }
    }

    private suspend fun loadStudyPlanCard(): DashboardStudyPlanState {
        return try {
            when (val plansResult = studyPlannerRepository.listPlans()) {
                is Resource.Success -> {
                    val activePlan = plansResult.data.firstOrNull()
                    if (activePlan == null) {
                        DashboardStudyPlanState()
                    } else {
                        val calendarResult = studyPlannerRepository.getCalendar(activePlan.id)
                        val calendar = when (calendarResult) {
                            is Resource.Success -> calendarResult.data
                            is Resource.Error -> emptyMap()
                            is Resource.Loading -> emptyMap()
                        }
                        val calendarError = (calendarResult as? Resource.Error)?.message
                        buildDashboardStudyPlanState(activePlan, calendar, errorMessage = calendarError)
                    }
                }
                is Resource.Error -> DashboardStudyPlanState(errorMessage = plansResult.message)
                is Resource.Loading -> DashboardStudyPlanState()
            }
        } catch (e: Exception) {
            DashboardStudyPlanState(errorMessage = e.localizedMessage ?: "Could not load study planner.")
        }
    }
}
