package com.safar.app.ui.home

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
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val userAvatar: String? = null,
    val activeTitle: String = "",
    val streaks: Streaks = Streaks(0, 0, 0),
    val todayMood: Mood? = null,
    val todayGoals: List<Goal> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
    val monthlyReport: MonthlyReport? = null,
    val weeklyMoods: List<Mood> = emptyList(),
    val earnedAchievements: List<Achievement> = emptyList(),
    val allAchievements: List<Achievement> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val dataStore: SafarDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val streaksDeferred      = async { homeRepository.getStreaks() }
            val moodsDeferred        = async { homeRepository.getMoods() }
            val goalsDeferred        = async { homeRepository.getGoals() }
            val reportDeferred       = async { homeRepository.getMonthlyReport() }
            val titleDeferred        = async { homeRepository.getActiveTitle() }
            val achievementsDeferred = async { homeRepository.getAchievements() }

            val userName  = runCatching { dataStore.userName.first() }.getOrDefault("")
            val userAvatar = runCatching { dataStore.userAvatar.first() }.getOrDefault(null)

            val streaks      = (streaksDeferred.await()      as? Resource.Success)?.data ?: Streaks(0, 0, 0)
            val moods        = (moodsDeferred.await()        as? Resource.Success)?.data ?: emptyList()
            val goals        = (goalsDeferred.await()        as? Resource.Success)?.data ?: emptyList()
            val report       = (reportDeferred.await()       as? Resource.Success)?.data
            val title        = (titleDeferred.await()        as? Resource.Success)?.data
            val achievements = (achievementsDeferred.await() as? Resource.Success)?.data ?: emptyList()

            val today = java.time.LocalDate.now().toString()
            val todayGoals = goals.filter { it.scheduledDate?.startsWith(today) == true }
            val completedGoals = goals.filter { it.completed }.takeLast(5)
            val todayMood = moods.firstOrNull { it.timestamp.startsWith(today) }
            val weeklyMoods = moods.take(7)

            _uiState.update {
                it.copy(
                    isLoading         = false,
                    userName          = userName ?: "",
                    userAvatar        = userAvatar,
                    activeTitle       = title?.title ?: "",
                    streaks           = streaks,
                    todayMood         = todayMood,
                    todayGoals        = todayGoals,
                    completedGoals    = completedGoals,
                    monthlyReport     = report,
                    weeklyMoods       = weeklyMoods,
                    earnedAchievements = achievements.filter { a -> a.earned },
                    allAchievements   = achievements
                )
            }
        }
    }
}