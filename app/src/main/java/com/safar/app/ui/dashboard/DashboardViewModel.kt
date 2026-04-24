package com.safar.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.domain.model.*
import com.safar.app.domain.repository.HomeRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val dataStore: SafarDataStore
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

            val today          = LocalDate.now().toString()          // "2026-03-29"
            val todayGoals     = goals.filter { it.scheduledDate?.startsWith(today) == true }
            val completedGoals = goals.filter { it.completed }.takeLast(5)
            val todayMood      = moods.firstOrNull { it.timestamp.startsWith(today) }
            val weeklyMoods    = moods.take(7)

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
                    showWelcomeOverlay    = !welcomeSeen
                )
            }
        }
    }
}
