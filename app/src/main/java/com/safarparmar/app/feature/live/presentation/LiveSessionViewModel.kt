package com.safarparmar.app.feature.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.feature.live.data.LiveSessionRepositoryContract
import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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


data class LiveChatUiMessage(
    val author: String,
    val text: String,
    val sentAt: String,
    val isMine: Boolean,
)

data class LiveChatUiState(
    val messages: List<LiveChatUiMessage> = emptyList(),
    // null = no error; non-null = error message to show user
    val socketError: String? = null,
    // true while we are connecting to the socket room
    val isConnecting: Boolean = false,
)

@HiltViewModel
class LiveSessionViewModel @Inject constructor(
    private val repository: LiveSessionRepositoryContract,
    private val socketManager: MehfilSocketManager,
    private val dataStore: SafarDataStore,
) : ViewModel() {
    private val _liveSessionsState = MutableStateFlow(LiveSessionsUiState(isLoading = true))
    val liveSessionsState: StateFlow<LiveSessionsUiState> = _liveSessionsState.asStateFlow()

    private val _liveSessionState = MutableStateFlow(LiveSessionUiState(isLoading = true))
    val liveSessionState: StateFlow<LiveSessionUiState> = _liveSessionState.asStateFlow()


    private val _liveChatState = MutableStateFlow(LiveChatUiState())
    val liveChatState: StateFlow<LiveChatUiState> = _liveChatState.asStateFlow()

    /** The session currently being watched for live chat (used to join/leave socket rooms). */
    private var activeSocketSessionId: String? = null
    private var currentUserName: String = "Student"
    private var pollingJob: Job? = null
    private var socketWatchJob: Job? = null

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

    /**
     * Called when the Live Session screen becomes visible.
     * Joins the socket room for this session and starts listening to real-time chat & status events.
     */
    fun joinLiveSession(sessionId: String) {
        if (activeSocketSessionId == sessionId) return // already joined
        leaveLiveSession() // leave any previous room

        activeSocketSessionId = sessionId
        _liveChatState.value = LiveChatUiState(isConnecting = true)

        viewModelScope.launch {
            currentUserName = dataStore.userName.first() ?: "Student"

            if (socketManager.isConnected()) {
                // Already connected — join immediately
                socketManager.emitLiveJoin(sessionId)
                _liveChatState.update { it.copy(isConnecting = false) }
            } else {
                // Not connected yet — show connecting state and wait
                _liveChatState.update { it.copy(isConnecting = true, socketError = null) }
            }
        }

        // Watch the connected state — join the room as soon as socket connects
        socketWatchJob?.cancel()
        socketWatchJob = viewModelScope.launch {
            socketManager.connected
                .distinctUntilChanged()
                .collect { isConnected ->
                    val sid = activeSocketSessionId ?: return@collect
                    if (isConnected) {
                        android.util.Log.d("LiveVM", "Socket connected — joining live room $sid")
                        socketManager.emitLiveJoin(sid)
                        _liveChatState.update { it.copy(isConnecting = false, socketError = null) }
                    } else {
                        _liveChatState.update { it.copy(isConnecting = true) }
                    }
                }
        }

        // Collect incoming chat messages from the server
        viewModelScope.launch {
            socketManager.liveMessage.collect { msg ->
                if (activeSocketSessionId == null) return@collect
                val isMine = msg.name == currentUserName
                _liveChatState.update { state ->
                    state.copy(
                        messages = state.messages + LiveChatUiMessage(
                            author = msg.name,
                            text = msg.text,
                            sentAt = msg.sentAt,
                            isMine = isMine,
                        ),
                    )
                }
            }
        }

        // Collect live:status_changed — reload session data so the player URL updates
        viewModelScope.launch {
            socketManager.liveStatusChanged.collect { change ->
                if (change.sessionId != activeSocketSessionId) return@collect
                // Refresh session from API so the updated youtubeEmbedUrl is picked up by the player
                loadSession(change.sessionId)
            }
        }

        // Collect live:error and surface as a dismissible UI message
        viewModelScope.launch {
            socketManager.liveError.collect { errorMsg ->
                if (activeSocketSessionId == null) return@collect
                _liveChatState.update { it.copy(socketError = errorMsg) }
            }
        }

        // Fallback polling — ONLY fires when the socket is offline.
        // When socket is healthy, students get the status_changed push event
        // and never poll at all (zero extra load on the VPS).
        // 30s base + random jitter spreads 90k students' requests across
        // ~30 seconds instead of all hitting simultaneously.
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                // Randomised interval: 25–55 seconds
                val jitter = (0..30_000L.toInt()).random().toLong()
                delay(25_000L + jitter)
                val sid = activeSocketSessionId ?: break
                val status = _liveSessionState.value.session?.status
                // Only poll if socket is NOT connected — if socket works, skip
                if (!socketManager.isConnected() && (status == "scheduled" || status == "live")) {
                    android.util.Log.d("LiveVM", "Socket offline — polling session status for $sid")
                    loadSession(sid)
                } else if (status != "scheduled" && status != "live") {
                    break // stop polling once session ended/cancelled
                }
                // If socket IS connected, skip this poll cycle — socket handles it
            }
        }
    }

    /** Sends a chat message to the active live session. */
    fun sendLiveMessage(text: String) {
        val sessionId = activeSocketSessionId ?: return
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        if (!socketManager.isConnected()) {
            _liveChatState.update { it.copy(
                socketError = "Live chat is temporarily unavailable. Please check your connection.",
            ) }
            return
        }

        socketManager.emitLiveMessage(
            sessionId = sessionId,
            name = currentUserName,
            text = trimmed,
        )
    }

    /** Called when the Live Session screen becomes invisible / user navigates away. */
    fun leaveLiveSession() {
        pollingJob?.cancel()
        pollingJob = null
        socketWatchJob?.cancel()
        socketWatchJob = null
        val sessionId = activeSocketSessionId ?: return
        socketManager.emitLiveLeave(sessionId)
        activeSocketSessionId = null
    }

    /** Clears a transient socket error so the UI can dismiss it. */
    fun clearLiveChatError() {
        _liveChatState.update { it.copy(socketError = null) }
    }


    override fun onCleared() {
        super.onCleared()
        leaveLiveSession()
    }
}
