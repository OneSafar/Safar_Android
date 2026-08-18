package com.safarparmar.app.ui.studycircle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.StudyCircleApi
import com.safarparmar.app.data.remote.dto.*
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response

data class StudyCircleHubState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val circles: List<StudyCircleSummaryDto> = emptyList(),
    val publicCircles: List<PublicStudyCircleDto> = emptyList(),
    val error: String? = null,
    val busyId: String? = null,
)

data class StudyCircleDetailState(
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val circle: StudyCircleDetailDto? = null,
    val leaderboard: StudyCircleLeaderboardResponse? = null,
    val error: String? = null,
    val actionInProgress: Boolean = false,
)

@HiltViewModel
class StudyCircleViewModel @Inject constructor(
    private val api: StudyCircleApi,
    val dataStore: SafarDataStore,
    val socketManager: MehfilSocketManager,
) : ViewModel() {
    private val _hub = MutableStateFlow(StudyCircleHubState())
    val hub = _hub.asStateFlow()
    private val _detail = MutableStateFlow(StudyCircleDetailState())
    val detail = _detail.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    val currentUserId = dataStore.userId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val mehfilDm = dataStore.premiumFeatureMehfilDm.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val isPremium = dataStore.isPremium.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init { loadHub() }

    fun consumeMessage() { _message.value = null }

    fun connectWithMember(
        targetUserId: String,
        targetUserName: String,
        circleName: String,
        onRequirePremium: () -> Unit,
        onConnected: () -> Unit,
    ) {
        viewModelScope.launch {
            val userId = dataStore.userId.firstOrNull() ?: ""
            val hasDmAccess = (dataStore.premiumFeatureMehfilDm.firstOrNull() ?: false) || (dataStore.isPremium.firstOrNull() ?: false)
            if (targetUserId.isBlank() || targetUserId == userId) {
                _message.value = "Cannot connect with yourself"
                return@launch
            }
            if (!hasDmAccess) {
                onRequirePremium()
                return@launch
            }
            if (!socketManager.isConnected()) {
                val token = dataStore.authToken.firstOrNull() ?: ""
                val userName = dataStore.userName.firstOrNull() ?: "Safarite"
                val avatar = dataStore.userAvatar.firstOrNull()
                if (token.isNotEmpty() && userId.isNotEmpty()) {
                    socketManager.connect(token, userId, userName, avatar)
                }
            }
            socketManager.emitDmRequest(
                targetUserId = targetUserId,
                contextPreview = "Study Circle: $circleName",
            )
            _message.value = "Connection request sent to $targetUserName"
            onConnected()
        }
    }

    fun loadHub(refresh: Boolean = false) = viewModelScope.launch {
        val previous = _hub.value
        _hub.value = _hub.value.copy(loading = !refresh, refreshing = refresh, error = null)
        try {
            val mine = async { timedApiCall("Your circles") { api.getMyCircles() } }
            val public = async { timedApiCall("Public circles") { api.getPublicCircles() } }
            val mineResult = mine.await()
            val publicResult = public.await()
            val mineData = (mineResult as? Resource.Success)?.data?.circles
            val publicData = (publicResult as? Resource.Success)?.data?.circles

            if (mineData != null || publicData != null) {
                _hub.value = _hub.value.copy(
                    circles = mineData ?: previous.circles,
                    publicCircles = publicData ?: previous.publicCircles,
                    error = null,
                )
                val partialError = (mineResult as? Resource.Error)?.message
                    ?: (publicResult as? Resource.Error)?.message
                if (partialError != null) _message.value = partialError
            } else {
                _hub.value = _hub.value.copy(
                    error = (mineResult as? Resource.Error)?.message
                        ?: (publicResult as? Resource.Error)?.message
                        ?: "Study Circles could not be loaded.",
                )
            }
        } finally {
            _hub.value = _hub.value.copy(loading = false, refreshing = false)
        }
    }

    fun createCircle(name: String, visibility: String, onOpened: (String) -> Unit) = action("create") {
        when (val result = safeApiCall { api.createCircle(CreateStudyCircleRequest(name.trim(), visibility)) }) {
            is Resource.Success -> { _message.value = "Study Circle created"; onOpened(result.data.circle.id) }
            is Resource.Error -> _message.value = result.message
            else -> Unit
        }
    }

    fun joinWithCode(code: String, onOpened: (String) -> Unit) = action("code") {
        when (val result = safeApiCall { api.joinWithCode(JoinStudyCircleRequest(code.trim().uppercase())) }) {
            is Resource.Success -> { _message.value = if (result.data.alreadyMember) "You are already in this circle" else "Joined Study Circle"; onOpened(result.data.circleId) }
            is Resource.Error -> _message.value = result.message
            else -> Unit
        }
    }

    fun joinPublic(circle: PublicStudyCircleDto, onOpened: (String) -> Unit) {
        if (circle.joined) { onOpened(circle.id); return }
        action(circle.id) {
            when (val result = safeApiCall { api.joinPublic(circle.id) }) {
                is Resource.Success -> { _message.value = "Joined public Study Circle"; onOpened(result.data.circleId) }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun loadDetail(circleId: String, refresh: Boolean = false) = viewModelScope.launch {
        _detail.value = _detail.value.copy(loading = !refresh, refreshing = refresh, error = null)
        try {
            val circle = async { timedApiCall("Study Circle") { api.getCircle(circleId) } }
            val board = async { timedApiCall("Leaderboard") { api.getLeaderboard(circleId) } }
            val circleResult = circle.await()
            val boardResult = board.await()
            if (circleResult is Resource.Success && boardResult is Resource.Success) {
                _detail.value = StudyCircleDetailState(circle = circleResult.data.circle, leaderboard = boardResult.data)
            } else {
                _detail.value = _detail.value.copy(
                    error = (circleResult as? Resource.Error)?.message
                        ?: (boardResult as? Resource.Error)?.message
                        ?: "This Study Circle could not be loaded.",
                )
            }
        } finally {
            _detail.value = _detail.value.copy(loading = false, refreshing = false)
        }
    }

    fun toggleVisibility() {
        val circle = _detail.value.circle ?: return
        val next = if (circle.visibility == "public") "private" else "public"
        detailAction {
            when (val result = safeApiCall { api.setVisibility(circle.id, SetStudyCircleVisibilityRequest(next)) }) {
                is Resource.Success -> {
                    _detail.value = _detail.value.copy(circle = circle.copy(visibility = result.data.visibility))
                    _message.value = if (next == "public") "Anyone can now find and join this circle" else "Circle is now private and code-only"
                }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun leaveCircle(onLeft: () -> Unit) {
        val circle = _detail.value.circle ?: return
        detailAction {
            when (val result = safeApiCall { api.leaveCircle(circle.id) }) {
                is Resource.Success -> { _message.value = if (circle.memberCount == 1) "Circle archived" else "You left the circle"; onLeft() }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun renameCircle(newName: String, onRenamed: ((previousName: String) -> Unit)? = null) {
        val circle = _detail.value.circle ?: return
        val trimmed = newName.trim()
        if (trimmed.length < 3 || trimmed.length > 50) {
            _message.value = "Circle name must be between 3 and 50 characters."
            return
        }
        val previousName = circle.name
        if (trimmed == previousName) return

        detailAction {
            when (val result = safeApiCall { api.updateCircleName(circle.id, UpdateStudyCircleNameRequest(trimmed)) }) {
                is Resource.Success -> {
                    _detail.value = _detail.value.copy(circle = circle.copy(name = result.data.name))
                    _message.value = "Group name updated to \"${result.data.name}\""
                    onRenamed?.invoke(previousName)
                }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun deleteCircle(onDeleted: () -> Unit) {
        val circle = _detail.value.circle ?: return
        detailAction {
            when (val result = safeApiCall { api.deleteCircle(circle.id) }) {
                is Resource.Success -> {
                    _message.value = "Study group deleted"
                    onDeleted()
                }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    fun removeMember(userId: String, name: String) {
        val circle = _detail.value.circle ?: return
        detailAction {
            when (val result = safeApiCall { api.removeMember(circle.id, userId) }) {
                is Resource.Success -> { _message.value = "$name was removed"; loadDetail(circle.id, refresh = true) }
                is Resource.Error -> _message.value = result.message
                else -> Unit
            }
        }
    }

    private fun action(id: String, block: suspend () -> Unit) = viewModelScope.launch {
        _hub.value = _hub.value.copy(busyId = id)
        try { block() } finally { _hub.value = _hub.value.copy(busyId = null) }
    }

    private fun detailAction(block: suspend () -> Unit) = viewModelScope.launch {
        _detail.value = _detail.value.copy(actionInProgress = true)
        try { block() } finally { _detail.value = _detail.value.copy(actionInProgress = false) }
    }

    private suspend fun <T> timedApiCall(
        label: String,
        call: suspend () -> Response<T>,
    ): Resource<T> = withTimeoutOrNull(REQUEST_TIMEOUT_MS) {
        safeApiCall(call)
    } ?: Resource.Error("$label took too long to load. Please try again.")

    private companion object {
        const val REQUEST_TIMEOUT_MS = 15_000L
    }
}
