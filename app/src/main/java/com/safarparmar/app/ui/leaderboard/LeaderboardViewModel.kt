package com.safarparmar.app.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.LeaderboardApi
import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardResponseDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Success(
        val data: WeeklyLeaderboardResponseDto,
        val page: Int,
        val isRefreshing: Boolean = false,
    ) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val api: LeaderboardApi,
    val dataStore: SafarDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    val currentUserId: StateFlow<String?> = dataStore.userId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    private var currentPage: Int = 1

    init {
        loadPage(page = 1, showLoading = true)
    }

    fun loadPage(page: Int, showLoading: Boolean = false) {
        val targetPage = page.coerceIn(1, 5)
        currentPage = targetPage

        viewModelScope.launch {
            val currentState = _uiState.value
            if (showLoading || currentState !is LeaderboardUiState.Success) {
                _uiState.value = LeaderboardUiState.Loading
            } else {
                _uiState.value = currentState.copy(isRefreshing = true)
            }

            try {
                val response = api.getWeeklyLeaderboard(page = targetPage)
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = LeaderboardUiState.Success(
                        data = response.body()!!,
                        page = targetPage,
                        isRefreshing = false,
                    )
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Failed to load leaderboard"
                    if (currentState is LeaderboardUiState.Success) {
                        _uiState.value = currentState.copy(isRefreshing = false)
                    } else {
                        _uiState.value = LeaderboardUiState.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                if (currentState is LeaderboardUiState.Success) {
                    _uiState.value = currentState.copy(isRefreshing = false)
                } else {
                    _uiState.value = LeaderboardUiState.Error(
                        e.localizedMessage ?: "Network error. Please check your connection."
                    )
                }
            }
        }
    }

    fun refresh() {
        loadPage(page = currentPage, showLoading = false)
    }

    fun jumpToUserPage() {
        val state = _uiState.value as? LeaderboardUiState.Success ?: return
        val rank = state.data.currentUserRank ?: return
        if (rank <= 0) return
        val pageSize = if (state.data.pageSize > 0) state.data.pageSize else 20
        val targetPage = ((rank - 1) / pageSize) + 1
        if (targetPage != currentPage && targetPage in 1..5) {
            loadPage(targetPage, showLoading = true)
        }
    }
}
