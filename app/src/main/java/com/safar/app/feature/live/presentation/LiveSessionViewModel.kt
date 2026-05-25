package com.safar.app.feature.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.feature.live.data.LiveSessionRepositoryContract
import com.safar.app.feature.live.model.LiveSession
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveSessionsUiState(
    val isLoading: Boolean = false,
    val sessions: List<LiveSession> = emptyList(),
    val errorMessage: String? = null,
    val errorCode: Int? = null,
)

data class LiveSessionUiState(
    val isLoading: Boolean = false,
    val session: LiveSession? = null,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
)

data class LiveSessionActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
    val errorCode: Int? = null,
)

@HiltViewModel
class LiveSessionViewModel @Inject constructor(
    private val repository: LiveSessionRepositoryContract,
) : ViewModel() {
    private val _liveSessionsState = MutableStateFlow(LiveSessionsUiState(isLoading = true))
    val liveSessionsState: StateFlow<LiveSessionsUiState> = _liveSessionsState.asStateFlow()

    private val _liveSessionState = MutableStateFlow(LiveSessionUiState(isLoading = true))
    val liveSessionState: StateFlow<LiveSessionUiState> = _liveSessionState.asStateFlow()

    private val _actionState = MutableStateFlow(LiveSessionActionState())
    val actionState: StateFlow<LiveSessionActionState> = _actionState.asStateFlow()

    fun loadSessions(courseId: String, status: String?) {
        viewModelScope.launch {
            _liveSessionsState.value = _liveSessionsState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.listByCourse(courseId, status)) {
                is Resource.Success -> _liveSessionsState.value = LiveSessionsUiState(sessions = result.data)
                is Resource.Error -> _liveSessionsState.value = LiveSessionsUiState(errorMessage = result.message, errorCode = result.code)
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadSession(id: String) {
        viewModelScope.launch {
            _liveSessionState.value = _liveSessionState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getById(id)) {
                is Resource.Success -> _liveSessionState.value = LiveSessionUiState(session = result.data)
                is Resource.Error -> _liveSessionState.value = LiveSessionUiState(errorMessage = result.message, errorCode = result.code)
                is Resource.Loading -> Unit
            }
        }
    }

    fun startLiveSession(id: String, youtubeUrl: String, courseId: String, status: String?) {
        val trimmedUrl = youtubeUrl.trim()
        if (trimmedUrl.isBlank()) {
            _actionState.value = LiveSessionActionState(errorMessage = "Paste a YouTube Live URL first.")
            return
        }

        viewModelScope.launch {
            _actionState.value = LiveSessionActionState(isLoading = true)
            when (val result = repository.startLiveSession(id, trimmedUrl)) {
                is Resource.Success -> {
                    _actionState.value = LiveSessionActionState(message = "Live class started.")
                    loadSessions(courseId, status)
                }
                is Resource.Error -> _actionState.value = LiveSessionActionState(
                    errorMessage = result.message,
                    errorCode = result.code,
                )
                is Resource.Loading -> Unit
            }
        }
    }

    fun endLiveSession(id: String, courseId: String, status: String?, recordingVideoId: String? = null) {
        viewModelScope.launch {
            _actionState.value = LiveSessionActionState(isLoading = true)
            when (val result = repository.endLiveSession(id, recordingVideoId)) {
                is Resource.Success -> {
                    _actionState.value = LiveSessionActionState(message = "Live class ended.")
                    loadSessions(courseId, status)
                }
                is Resource.Error -> _actionState.value = LiveSessionActionState(
                    errorMessage = result.message,
                    errorCode = result.code,
                )
                is Resource.Loading -> Unit
            }
        }
    }

    fun clearActionMessage() {
        _actionState.value = LiveSessionActionState()
    }
}
