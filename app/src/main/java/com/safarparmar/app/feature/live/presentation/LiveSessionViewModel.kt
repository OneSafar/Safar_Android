package com.safarparmar.app.feature.live.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.feature.live.data.LiveSessionRepositoryContract
import com.safarparmar.app.feature.live.data.LiveSocketConnector
import com.safarparmar.app.feature.live.model.LiveSession
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    /**
     * Live chat is one shared lane, so this never changes a message's position —
     * it only lets a student pick their own name out of the stream.
     */
    val isMine: Boolean,
    /** The person presenting. Shown with an owner badge, as on a live stream. */
    val isHost: Boolean = false,
)

data class LiveChatUiState(
    val messages: List<LiveChatUiMessage> = emptyList(),
    // null = no error; non-null = error message to show user
    val socketError: String? = null,
    // true while we are connecting to the socket room
    val isConnecting: Boolean = false,
    /**
     * Whether comments are accepted right now. Open only while the session is
     * actually broadcasting and the host hasn't switched chat off; the server
     * enforces the same rule, this just keeps the UI honest.
     */
    val isChatOpen: Boolean = false,
    /** Seconds left before this student may comment again; 0 means they can send now. */
    val cooldownRemainingSeconds: Int = 0,
    /** The gap the server enforces between one student's comments. */
    val cooldownSeconds: Int = DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS,
    /**
     * How many distinct people are watching right now. Counted per person, not per
     * connection, so one student on phone and laptop is one viewer.
     */
    val viewerCount: Int = 0,
) {
    val canSend: Boolean get() = isChatOpen && cooldownRemainingSeconds == 0
}

/**
 * Matches the server's `LIVE_CHAT_COOLDOWN_MS`. Used until the server tells us its
 * own value on join, so the very first send is never wrongly allowed.
 */
const val DEFAULT_LIVE_CHAT_COOLDOWN_SECONDS = 7

/** Matches the server's own truncation of `live:message` text. */
const val MAX_LIVE_MESSAGE_LENGTH = 500

/**
 * Live chat is not persisted, so the transcript only ever grows within a session.
 * A busy class would otherwise keep every message in memory for hours.
 */
private const val MAX_RETAINED_MESSAGES = 300

/**
 * Live chat exists only for the duration of a broadcast: a scheduled, ended or
 * cancelled session has none, and the host can switch it off mid-session.
 */
fun isChatOpen(session: LiveSession?): Boolean =
    session != null && session.status.equals("live", ignoreCase = true) && session.isChatEnabled

@HiltViewModel
class LiveSessionViewModel @Inject constructor(
    private val repository: LiveSessionRepositoryContract,
    private val socketManager: MehfilSocketManager,
    private val dataStore: SafarDataStore,
    private val socketConnector: LiveSocketConnector,
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
    private var currentUserId: String = ""
    private var pollingJob: Job? = null
    private var socketWatchJob: Job? = null
    private var cooldownJob: Job? = null

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
                is Resource.Success -> {
                    _liveSessionState.value = LiveSessionUiState(session = result.data)
                    applyChatOpen(isChatOpen(result.data))
                }
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
        cooldownJob?.cancel()
        _liveChatState.value = LiveChatUiState(
            isConnecting = true,
            // Seeded from the session we already loaded; the server confirms or
            // corrects this with live:chat_state the moment we join the room.
            isChatOpen = isChatOpen(_liveSessionState.value.session),
        )

        viewModelScope.launch {
            currentUserName = dataStore.userName.first() ?: "Student"
            currentUserId = dataStore.userId.first().orEmpty()

            if (socketManager.isConnected()) {
                // Already connected — join immediately
                socketManager.emitLiveJoin(sessionId)
                _liveChatState.update { it.copy(isConnecting = false) }
            } else {
                // Nothing else opens the socket for this screen. It used to be
                // connected only by MehfilViewModel, so a student who came
                // straight to a live session sat on "Connecting…" forever and
                // every send failed with "chat is temporarily unavailable".
                _liveChatState.update { it.copy(isConnecting = true, socketError = null) }
                connectSocket()
            }
        }

        // Watch the connected state — join the room as soon as socket connects
        socketWatchJob?.cancel()
        socketWatchJob = viewModelScope.launch {
            socketManager.connected
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
                // Matched on user id, not display name: two students called
                // "Safar" used to have each other's comments marked as their own.
                val isMine = msg.userId.isNotBlank() && msg.userId == currentUserId
                _liveChatState.update { state ->
                    state.copy(
                        messages = (
                            state.messages + LiveChatUiMessage(
                                author = msg.name,
                                text = msg.text,
                                sentAt = msg.sentAt,
                                isMine = isMine,
                                isHost = msg.isHost,
                            )
                            ).takeLast(MAX_RETAINED_MESSAGES),
                    )
                }
            }
        }

        // Collect live:status_changed — reload session data so the player URL updates
        viewModelScope.launch {
            socketManager.liveStatusChanged.collect { change ->
                if (change.sessionId != activeSocketSessionId) return@collect
                // Ending the broadcast ends the conversation: nothing is persisted
                // server-side, so leaving the transcript on screen would show a
                // chat the student can no longer take part in or ever get back.
                if (!change.status.equals("live", ignoreCase = true)) {
                    _liveChatState.update {
                        it.copy(messages = emptyList(), isChatOpen = false, cooldownRemainingSeconds = 0)
                    }
                    cooldownJob?.cancel()
                }
                // Refresh session from API so the updated youtubeEmbedUrl is picked up by the player
                loadSession(change.sessionId)
            }
        }

        // Collect live:chat_state — the server's authoritative open/closed verdict
        viewModelScope.launch {
            socketManager.liveChatState.collect { state ->
                if (state.sessionId != activeSocketSessionId) return@collect
                if (!state.isChatOpen) cooldownJob?.cancel()
                _liveChatState.update {
                    it.copy(
                        isChatOpen = state.isChatOpen,
                        messages = if (state.isChatOpen) it.messages else emptyList(),
                        cooldownRemainingSeconds = if (state.isChatOpen) it.cooldownRemainingSeconds else 0,
                        cooldownSeconds = state.cooldownSeconds.takeIf { seconds -> seconds > 0 }
                            ?: it.cooldownSeconds,
                    )
                }
            }
        }

        // Collect live:viewers — how many people are watching right now
        viewModelScope.launch {
            socketManager.liveViewerCount.collect { viewers ->
                if (viewers.sessionId != activeSocketSessionId) return@collect
                _liveChatState.update { it.copy(viewerCount = viewers.count) }
            }
        }

        // Collect live:error and surface as a dismissible UI message
        viewModelScope.launch {
            socketManager.liveError.collect { error ->
                if (activeSocketSessionId == null) return@collect
                when (error.code) {
                    // The server refused because this student is still in their gap.
                    // Re-arm the local countdown from the server's number so the two
                    // never drift apart.
                    "RATE_LIMITED" -> startCooldown(
                        ((error.retryAfterMs + 999L) / 1000L).toInt()
                            .coerceAtLeast(1),
                    )
                    "CHAT_CLOSED" -> _liveChatState.update {
                        it.copy(isChatOpen = false, socketError = error.message)
                    }
                    else -> _liveChatState.update { it.copy(socketError = error.message) }
                }
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

    /**
     * Sends a chat message to the active live session.
     *
     * Refuses locally when chat is closed or the student is still inside their gap,
     * so the common case never costs a round-trip that the server would only reject.
     */
    fun sendLiveMessage(text: String) {
        val sessionId = activeSocketSessionId ?: return
        val trimmed = text.trim().take(MAX_LIVE_MESSAGE_LENGTH)
        if (trimmed.isBlank()) return

        val chat = _liveChatState.value
        if (!chat.isChatOpen) {
            _liveChatState.update {
                it.copy(socketError = "Live chat is open only while the session is live.")
            }
            return
        }
        if (chat.cooldownRemainingSeconds > 0) return

        if (!socketManager.isConnected()) {
            // Try to recover rather than only complaining: the socket may have
            // dropped while the screen was backgrounded.
            _liveChatState.update { it.copy(
                isConnecting = true,
                socketError = "Reconnecting to live chat — try again in a moment.",
            ) }
            viewModelScope.launch { connectSocket() }
            return
        }

        socketManager.emitLiveMessage(
            sessionId = sessionId,
            name = currentUserName,
            text = trimmed,
        )
        // Start the gap on send rather than on the echo coming back: the student
        // should see the wait begin the instant they tap, and a dropped echo must
        // not leave the composer free to spam.
        //
        // The extra second covers network latency. The server's window opens from
        // when it *received* the message, so a local timer started at tap always
        // expires slightly early — without the padding the next send would race the
        // server and come back rejected, bouncing the countdown back up.
        startCooldown(chat.cooldownSeconds + 1)
    }

    private suspend fun connectSocket() {
        when (val result = socketConnector.ensureConnected()) {
            is LiveSocketConnector.Result.Connecting -> Unit
            is LiveSocketConnector.Result.SignInRequired -> _liveChatState.update {
                it.copy(isConnecting = false, socketError = result.message)
            }
        }
    }

    /** Runs the visible countdown that gates the composer. */
    private fun startCooldown(seconds: Int) {
        val total = seconds.coerceAtLeast(1)
        cooldownJob?.cancel()
        _liveChatState.update { it.copy(cooldownRemainingSeconds = total) }
        cooldownJob = viewModelScope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1_000L)
                remaining -= 1
                _liveChatState.update { it.copy(cooldownRemainingSeconds = remaining.coerceAtLeast(0)) }
            }
        }
    }

    private fun applyChatOpen(open: Boolean) {
        if (!open) cooldownJob?.cancel()
        _liveChatState.update {
            it.copy(
                isChatOpen = open,
                messages = if (open) it.messages else emptyList(),
                cooldownRemainingSeconds = if (open) it.cooldownRemainingSeconds else 0,
            )
        }
    }

    /** Called when the Live Session screen becomes invisible / user navigates away. */
    fun leaveLiveSession() {
        pollingJob?.cancel()
        pollingJob = null
        socketWatchJob?.cancel()
        socketWatchJob = null
        cooldownJob?.cancel()
        cooldownJob = null
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
