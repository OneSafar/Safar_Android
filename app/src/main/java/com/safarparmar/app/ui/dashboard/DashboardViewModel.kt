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
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import com.safarparmar.app.notifications.SafarNotificationManager
import com.safarparmar.app.notifications.SafarNotificationChannels
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

    fun selectAchievement(achievementId: String?) {
        viewModelScope.launch(exceptionHandler) {
            when (val result = homeRepository.selectAchievement(achievementId)) {
                is Resource.Success -> {
                    val selId = result.data.selectedId
                    val imgPath = selId.takeIf { it.isNotEmpty() }?.let { achievementImages[it] }
                    _uiState.update {
                        it.copy(
                            activeTitle = result.data.title,
                            activeTitleId = selId,
                            activeTitleImageUrl = resolveImageUrl(imgPath)
                        )
                    }
                }
                is Resource.Error -> _uiState.update { it.copy(error = result.message ?: "Failed to set active achievement") }
                is Resource.Loading -> Unit
            }
        }
    }

    private fun loadAll() {
        viewModelScope.launch(exceptionHandler) {
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
            
            // Align and pad moods for the current week (Monday to Sunday)
            val todayDate      = LocalDate.now()
            val dayOfWeekVal   = todayDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
            val mondayDate     = todayDate.minusDays((dayOfWeekVal - 1).toLong())
            val weeklyMoods    = (0..6).map { i ->
                val dateStr = mondayDate.plusDays(i.toLong()).toString()
                moods.firstOrNull { it.timestamp.startsWith(dateStr) } ?: Mood(intensity = 0, mood = "", timestamp = dateStr)
            }

            // Trigger local notifications for newly earned achievements
            val notifiedAchievements = dataStore.notifiedAchievements.first()
            val notificationsEnabled = dataStore.notificationsEnabled.first() && dataStore.achievementsEnabled.first()
            val newlyEarned = achievements.filter { it.earned && !notifiedAchievements.contains(it.id) }
            
            if (newlyEarned.isNotEmpty()) {
                if (notificationsEnabled) {
                    val notificationManager = SafarNotificationManager(context)
                    newlyEarned.forEach { achievement ->
                        notificationManager.show(
                            title = "Achievement Unlocked! 🏆",
                            body = "You unlocked: ${achievement.name}",
                            channelId = SafarNotificationChannels.ACHIEVEMENTS,
                            deepLink = "safar://achievements"
                        )
                    }
                }
                viewModelScope.launch {
                    newlyEarned.forEach { dataStore.addNotifiedAchievement(it.id) }
                }
            }

            val titleId = title?.selectedId ?: ""
            val activeTitleImgPath = titleId.takeIf { it.isNotEmpty() }?.let { achievementImages[it] }
            val activeTitleImgUrl = resolveImageUrl(activeTitleImgPath)

            _uiState.update {
                it.copy(
                    isLoading          = false,
                    userName           = userName ?: "",
                    userAvatar         = userAvatar,
                    activeTitle        = title?.title ?: "",
                    activeTitleId      = titleId,
                    activeTitleImageUrl = activeTitleImgUrl,
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
                    showWelcomeOverlay    = !welcomeSeen,
                    celebrationAchievements = newlyEarned
                )
            }
        }
    }

    private fun resolveImageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return try {
            val origin = com.safarparmar.app.BuildConfig.BASE_URL.trimEnd('/').let {
                val uri = android.net.Uri.parse(it)
                "${uri.scheme}://${uri.host}"
            }
            "$origin$path"
        } catch (_: Exception) {
            null
        }
    }

    private val achievementImages: Map<String, String> = mapOf(
        "G001" to "/Achievments/Badges/Badge (1).webp",
        "G002" to "/Achievments/Badges/Badge (2).webp",
        "G003" to "/Achievments/Badges/Badge (3).webp",
        "G004" to "/Achievments/Badges/Badge (4).webp",
        "F001" to "/Achievments/Badges/Special_Badge (2).webp",
        "F002" to "/Achievments/Badges/Special_Badge (5).webp",
        "F003" to "/Achievments/Badges/Special_Badge (4).webp",
        "F004" to "/Achievments/Badges/Badge (6).webp",
        "F005" to "/Achievments/Badges/Badge (7).webp",
        "S001" to "/Achievments/Badges/Badge (8).webp",
        "S002" to "/Achievments/Badges/Special_Badge (1).webp",
        "ET006" to "/Achievments/Badges/Special_Badge (3).webp",
        "SP001" to "/Achievments/Badges/Badge (4).webp",
        "SP002" to "/Achievments/Badges/Badge (8).webp",
        "D001" to "/Achievments/Badges/Special_Badge (1).webp",
        "D002" to "/Achievments/Badges/Special_Badge (3).webp",
        "K001" to "/Achievments/Badges/Badge (6).webp",
        "M001" to "/Achievments/Badges/Badge (7).webp",
        "T005" to "/Achievments/Titles/Title (5).webp",
        "T006" to "/Achievments/Titles/Title (3).webp",
        "T007" to "/Achievments/Titles/Title (7).webp",
        "T008" to "/Achievments/Titles/Title (6).webp",
        "T001" to "/Achievments/Titles/Title (8).webp",
        "T002" to "/Achievments/Titles/Title (2).webp",
        "T003" to "/Achievments/Titles/Title (1).webp",
        "T004" to "/Achievments/Titles/Title (4).webp",
        "ET001" to "/Achievments/Titles/Special_Title (2).webp",
        "ET002" to "/Achievments/Titles/Special_Title (1).webp",
        "ET003" to "/Achievments/Titles/Special_Title (5).webp",
        "ET004" to "/Achievments/Titles/Special_Title (3).webp",
        "ET005" to "/Achievments/Titles/Special_Title (4).webp",
        "T010" to "/Achievments/Titles/Special_Title (1).webp",
        "T011" to "/Achievments/Titles/Special_Title (2).webp",
        "T012" to "/Achievments/Titles/Special_Title (3).webp",
        "T013" to "/Achievments/Titles/Special_Title (4).webp",
        "T014" to "/Achievments/Titles/Special_Title (5).webp",
        "T009" to "/Achievments/svgviewer-output.svg",
    )

    private suspend fun loadStudyPlanCard(): DashboardStudyPlanState {
        return try {
            when (val plansResult = studyPlannerRepository.listPlans()) {
                is Resource.Success -> {
                    val summaryPlan = plansResult.data.firstOrNull()
                    if (summaryPlan == null) {
                        DashboardStudyPlanState()
                    } else {
                        // listPlans() returns summaries without the topic tree, so rollup()
                        // on them yields "0 of 0". Hydrate the full plan (like the planner's
                        // openPlan does) before computing progress; fall back to the summary
                        // if the detail fetch fails so the card still renders.
                        val activePlan = when (val planResult = studyPlannerRepository.getPlan(summaryPlan.id)) {
                            is Resource.Success -> planResult.data
                            else -> summaryPlan
                        }
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
            DashboardStudyPlanState(errorMessage = e.localizedMessage ?: "Could not load exam planner.")
        }
    }

    fun dismissCelebration() {
        _uiState.update { it.copy(celebrationAchievements = emptyList()) }
    }
}
