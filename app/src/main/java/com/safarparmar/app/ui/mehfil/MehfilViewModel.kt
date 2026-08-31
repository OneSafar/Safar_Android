package com.safarparmar.app.ui.mehfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safarparmar.app.data.local.SafarDataStore
import com.safarparmar.app.data.remote.api.StudyCircleApi
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.data.remote.socket.MehfilSocketManager
import com.safarparmar.app.data.remote.socket.toDomain
import com.safarparmar.app.domain.model.*
import com.safarparmar.app.data.repository.PremiumRepository
import com.safarparmar.app.domain.repository.MehfilRepository
import com.safarparmar.app.domain.repository.AuthRepository
import com.safarparmar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class DmMessageState { SENDING, SENT, FAILED }
data class DmMessage(
    val text: String,
    val isMine: Boolean,
    val senderAvatar: String? = null,
    val id: String = java.util.UUID.randomUUID().toString(),
    val state: DmMessageState = DmMessageState.SENT,
)

sealed class DmState {
    object Idle : DmState()
    data class Waiting(val userName: String) : DmState()
    data class IncomingRequest(val fromUserId: String, val fromUserName: String, val fromUserAvatar: String? = null) : DmState()
    data class Open(val peerId: String, val peerName: String, val roomId: String, val peerAvatar: String? = null, val messages: List<DmMessage> = emptyList()) : DmState()
}

data class PendingDmRequest(val userId: String, val userName: String, val requestId: String, val userAvatar: String? = null)

data class MehfilUiState(
    val isInitializing: Boolean = true,
    val isLoadingPosts: Boolean = false,
    val posts: List<MehfilPost> = emptyList(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasMore: Boolean = false,
    val latestSandesh: Sandesh? = null,
    val sandeshes: List<Sandesh> = emptyList(),
    val isPosting: Boolean = false,
    val postSuccess: Boolean = false,
    val postError: String? = null,
    val selectedSpace: String = "ALL",
    val onlineCount: Int = 0,
    val comments: List<Comment> = emptyList(),
    val isLoadingComments: Boolean = false,
    val isLoadingMoreComments: Boolean = false,
    val hasMoreComments: Boolean = false,
    val commentsPage: Int = 1,
    val currentCommentPostId: String = "",
    val isPostingComment: Boolean = false,
    val commentError: String? = null,
    val sandeshComments: List<Comment> = emptyList(),
    val isLoadingSandeshComments: Boolean = false,
    val isLoadingMoreSandeshComments: Boolean = false,
    val hasMoreSandeshComments: Boolean = false,
    val sandeshCommentsPage: Int = 1,
    val activity: List<ActivityItem> = emptyList(),
    val isLoadingActivity: Boolean = false,
    val savedPosts: List<MehfilPost> = emptyList(),
    val isLoadingSaved: Boolean = false,
    val savedPostIds: Set<String> = emptySet(),
    val savingPostIds: Set<String> = emptySet(),
    val userMessage: String? = null,
    val dmState: DmState = DmState.Idle,
    val dmError: String? = null,
    val dmTargetUserId: String? = null,
    val dmTargetUserName: String = "",
    val socketConnected: Boolean = false,
    val pendingDmRequests: List<PendingDmRequest> = emptyList(),
    val currentUserId: String = "",
    val currentUserAvatar: String? = null,
    val dmRequestId: String = "",
    val dmPeerOnline: Boolean = true,
    // Local overrides so optimistic like state survives list refreshes
    val localLikeOverrides: Map<String, Boolean> = emptyMap(),
    val localReactionOverrides: Map<String, Int> = emptyMap(),
    val reactedSandeshIds: Set<String> = emptySet(),
    val mehfilDm: Boolean = false,
    val isLoadingPremiumFeatures: Boolean = true,
    val showPremiumGate: Boolean = false,
    val studyCircles: List<StudyCircleSummaryDto> = emptyList(),
    val isLoadingStudyCircles: Boolean = false,
)

@HiltViewModel
class MehfilViewModel @Inject constructor(
    private val repo: MehfilRepository,
    private val socketManager: MehfilSocketManager,
    val dataStore: SafarDataStore,
    private val authRepo: AuthRepository,
    private val premiumRepository: PremiumRepository,
    private val studyCircleApi: StudyCircleApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MehfilUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSandesh()
        loadSavedPosts()
        loadPremiumFeatures()
        loadStudyCircles()
        initSocket()
    }

    private fun loadStudyCircles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStudyCircles = true) }
            runCatching { studyCircleApi.getMyCircles() }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            studyCircles = if (response.isSuccessful) response.body()?.circles.orEmpty() else emptyList(),
                            isLoadingStudyCircles = false,
                        )
                    }
                }
                .onFailure { _uiState.update { state -> state.copy(isLoadingStudyCircles = false) } }
        }
    }

    private fun loadPremiumFeatures() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPremiumFeatures = true) }
            launch {
                premiumRepository.cachedStatus.collect { status ->
                    _uiState.update {
                        it.copy(
                            mehfilDm = status.canUseMehfilDm,
                            isLoadingPremiumFeatures = false,
                        )
                    }
                }
            }
            premiumRepository.refreshStatus()
        }
    }

    fun dismissPremiumGate() {
        _uiState.update { it.copy(showPremiumGate = false) }
    }

    fun openPremiumGate() {
        _uiState.update { it.copy(showPremiumGate = true) }
    }

    private fun initSocket() {
        viewModelScope.launch {
            // A lightweight authenticated call lets the interceptor refresh an
            // expired access token before Socket.IO authenticates the handshake.
            authRepo.getMe()
            val token = dataStore.authToken.first() ?: run {
                _uiState.update { it.copy(isInitializing = false) }
                return@launch
            }

            var userId = dataStore.userId.first()
            if (userId.isNullOrBlank()) {
                android.util.Log.d("MehfilSocket", "userId null — fetching getMe()")
                authRepo.getMe()
                userId = dataStore.userId.first()
            }
            if (userId.isNullOrBlank()) {
                android.util.Log.w("MehfilSocket", "userId still null after getMe — aborting socket connect")
                _uiState.update { it.copy(isInitializing = false) }
                return@launch
            }

            val userName = dataStore.userName.first() ?: "Safarite"
            val avatar   = dataStore.userAvatar.first()

            _uiState.update { it.copy(isLoadingPosts = true, currentUserId = userId, currentUserAvatar = avatar) }

            socketManager.connect(
                token       = token,
                userId      = userId,
                userName    = userName,
                userAvatar  = avatar,
                initialRoom = _uiState.value.selectedSpace,
            )

            _uiState.update { it.copy(isInitializing = false) }

            launch {
                socketManager.connected.collect { connected ->
                    _uiState.update { it.copy(socketConnected = connected) }
                }
            }

            launch {
                socketManager.onlineCount.collect { count ->
                    _uiState.update { it.copy(onlineCount = count) }
                }
            }

            launch {
                socketManager.thoughtsEvent.collect { payload ->
                    val posts = payload.thoughts?.map { it.toDomain() } ?: return@collect
                    onThoughtsReceived(posts = posts, hasMore = payload.hasMore, page = payload.page)
                }
            }

            launch {
                socketManager.thoughtCreated.collect { post ->
                    val currentRoom = _uiState.value.selectedSpace
                    if (currentRoom == "ALL" || post.space.equals(currentRoom, ignoreCase = true)) {
                        addSocketPost(post)
                    }
                }
            }

            launch {
                socketManager.reactionUpdated.collect { (thoughtId, count, liked) ->
                    _uiState.update { state ->
                        state.copy(
                            posts = state.posts.map { post ->
                                if (post.id == thoughtId) post.copy(reactionCount = count, userLiked = liked) else post
                            },
                            localLikeOverrides = state.localLikeOverrides - thoughtId,
                            localReactionOverrides = state.localReactionOverrides - thoughtId,
                        )
                    }
                }
            }
            launch {
                socketManager.dmEvent.collect { event ->
                    when (event.type) {
                        "request_sent"     -> _uiState.update { it.copy(
                            dmRequestId = event.message,
                            dmState = DmState.Waiting(it.dmTargetUserName.ifBlank { "student" }),
                            dmError = null,
                        ) }
                        "incoming_request" -> _uiState.update { it.copy(
                            dmState = DmState.IncomingRequest(event.fromUserId, event.fromUserName, event.fromUserAvatar),
                            pendingDmRequests = listOf(PendingDmRequest(event.fromUserId, event.fromUserName, event.requestId.ifBlank { event.fromUserId }, event.fromUserAvatar)) +
                                it.pendingDmRequests.filterNot { p -> p.userId == event.fromUserId },
                        ) }
                        "opened", "accepted" -> _uiState.update { state ->
                            val existing = state.dmState as? DmState.Open
                            val peerId = event.fromUserId.ifBlank { state.dmTargetUserId.orEmpty() }
                            val peerName = event.fromUserName.ifBlank { state.dmTargetUserName }.ifBlank { peerId }.ifBlank { "Student" }
                            val avatar = event.fromUserAvatar ?: state.currentUserAvatar
                            state.copy(
                                dmState = DmState.Open(
                                    peerId = peerId,
                                    peerName = peerName,
                                    roomId = event.roomId,
                                    peerAvatar = avatar,
                                    messages = if (event.restored && existing?.roomId == event.roomId) existing.messages else emptyList(),
                                ),
                                dmTargetUserId = peerId,
                                dmTargetUserName = peerName,
                                dmPeerOnline = true,
                                dmError = null,
                                pendingDmRequests = state.pendingDmRequests.filterNot { p -> p.userId == peerId },
                            )
                        }
                        "declined"         -> _uiState.update { it.copy(dmState = DmState.Idle, dmError = "Request declined") }
                        "sync_pending"     -> _uiState.update { it.copy(pendingDmRequests = event.pendingList.map { id -> PendingDmRequest(id, id, id) }) }
                        "message"          -> {
                            val cur = _uiState.value.dmState
                            if (cur is DmState.Open) {
                                val isEcho = event.fromUserId.isBlank() || event.fromUserId == _uiState.value.currentUserId
                                if (isEcho) {
                                    _uiState.update { state ->
                                        val open = state.dmState as? DmState.Open ?: return@update state
                                        state.copy(dmState = open.copy(messages = open.messages.map { message ->
                                            if (message.id == event.clientMessageId) message.copy(state = DmMessageState.SENT) else message
                                        }))
                                    }
                                } else {
                                    val updatedPeerName = if (cur.peerName.isBlank() || cur.peerName == cur.peerId) event.fromUserName.ifBlank { cur.peerName } else cur.peerName
                                    val updatedPeerAvatar = event.fromUserAvatar ?: cur.peerAvatar
                                    _uiState.update { it.copy(dmState = cur.copy(peerName = updatedPeerName, peerAvatar = updatedPeerAvatar, messages = cur.messages + DmMessage(event.message, isMine = false, senderAvatar = event.fromUserAvatar))) }
                                }
                            }
                        }
                        "error" -> _uiState.update {
                            val showGate = event.errorCode == "PREMIUM_REQUIRED"
                            val open = it.dmState as? DmState.Open
                            it.copy(
                                dmError = event.message,
                                dmState = if (showGate) DmState.Idle else open?.copy(
                                    messages = open.messages.map { message ->
                                        if (message.state == DmMessageState.SENDING &&
                                            (event.clientMessageId.isBlank() || message.id == event.clientMessageId)
                                        ) message.copy(state = DmMessageState.FAILED) else message
                                    },
                                ) ?: it.dmState,
                                showPremiumGate = showGate || it.showPremiumGate,
                            )
                        }
                        "room_closed" -> _uiState.update { it.copy(dmState = DmState.Idle, dmError = event.message) }
                        "peer_offline" -> _uiState.update { it.copy(dmPeerOnline = false) }
                        "post_saved" -> _uiState.update { it.copy(isPosting = false, postSuccess = true, postError = null) }
                        "post_error" -> _uiState.update { it.copy(isPosting = false, postError = event.message) }
                    }
                }
            }
        }
    }

    private fun loadPostsFallback() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPosts = true) }
            _uiState.update {
                it.copy(
                    isLoadingPosts = false,
                    userMessage = "Could not load posts. Check your internet and try again.",
                )
            }
        }
    }

    fun joinRoom(room: String) {
        _uiState.update { it.copy(selectedSpace = room, posts = emptyList(), currentPage = 1, isLoadingPosts = true) }
        if (socketManager.isConnected()) {
            socketManager.joinRoomAndLoad(room)
        } else {
            viewModelScope.launch {
                val token = dataStore.authToken.first() ?: run {
                    _uiState.update { it.copy(isLoadingPosts = false, userMessage = "Please sign in again.") }
                    return@launch
                }
                var userId = dataStore.userId.first()
                if (userId.isNullOrBlank()) {
                    authRepo.getMe()
                    userId = dataStore.userId.first()
                }
                if (userId.isNullOrBlank()) {
                    _uiState.update { it.copy(isLoadingPosts = false, userMessage = "Could not open this room.") }
                    return@launch
                }
                val userName = dataStore.userName.first() ?: "Safarite"
                val avatar   = dataStore.userAvatar.first()
                socketManager.connect(token = token, userId = userId, userName = userName, userAvatar = avatar, initialRoom = room)
            }
        }
    }

    fun loadPosts(refresh: Boolean = false) {
        if (refresh) {
            _uiState.update { it.copy(isLoadingPosts = true, currentPage = 1) }
            if (socketManager.isConnected()) {
                socketManager.joinRoomAndLoad(_uiState.value.selectedSpace)
            } else {
                loadPostsFallback()
            }
        } else {
            val state = _uiState.value
            if (!state.hasMore || state.isLoadingPosts) return
            _uiState.update { it.copy(isLoadingPosts = true) }
            socketManager.loadThoughts(room = state.selectedSpace, page = state.currentPage + 1)
        }
    }

    fun onThoughtsReceived(posts: List<MehfilPost>, hasMore: Boolean, page: Int) {
        _uiState.update { state ->
            // Apply any local like overrides so optimistic state survives refresh
            val patched = posts.map { post ->
                val liked   = state.localLikeOverrides[post.id]
                val count   = state.localReactionOverrides[post.id]
                if (liked != null || count != null)
                    post.copy(userLiked = liked ?: post.userLiked, reactionCount = count ?: post.reactionCount)
                else post
            }
            // Deduplicate by id — keeps the latest version, prevents LazyColumn key crash
            val merged = if (page == 1) patched
                         else (state.posts + patched).distinctBy { it.id }
            state.copy(
                posts = merged,
                savedPostIds = (state.savedPostIds - patched.map { it.id }.toSet()) +
                    patched.filter { it.isSaved }.map { it.id },
                currentPage = page,
                totalPages = if (hasMore) page + 1 else page,
                hasMore = hasMore,
                isLoadingPosts = false,
            )
        }
    }

    fun toggleLike(post: MehfilPost) {
        socketManager.emitToggleReaction(post.id)
        _uiState.update { state ->
            val newLiked = !post.userLiked
            val newCount = if (post.userLiked) post.reactionCount - 1 else post.reactionCount + 1
            state.copy(
                posts = state.posts.map {
                    if (it.id == post.id) it.copy(userLiked = newLiked, reactionCount = newCount) else it
                },
                localLikeOverrides = state.localLikeOverrides + (post.id to newLiked),
                localReactionOverrides = state.localReactionOverrides + (post.id to newCount),
            )
        }
    }
    fun loadComments(thoughtId: String, loadMore: Boolean = false) {
        val state = _uiState.value
        if (loadMore && (!state.hasMoreComments || state.isLoadingMoreComments)) return
        val page = if (loadMore) state.commentsPage + 1 else 1
        viewModelScope.launch {
            if (loadMore) {
                _uiState.update { it.copy(isLoadingMoreComments = true) }
            } else {
                _uiState.update { it.copy(isLoadingComments = true, comments = emptyList(), commentsPage = 1, currentCommentPostId = thoughtId) }
            }
            when (val r = repo.getComments(thoughtId, page)) {
                is Resource.Success -> _uiState.update { s ->
                    s.copy(
                        isLoadingComments = false,
                        isLoadingMoreComments = false,
                        comments = if (loadMore) s.comments + r.data else r.data,
                        commentsPage = page,
                        hasMoreComments = r.data.size >= 20,
                    )
                }
                is Resource.Error   -> _uiState.update {
                    it.copy(isLoadingComments = false, isLoadingMoreComments = false, commentError = "Could not load comments. Try again.")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun postComment(thoughtId: String, content: String, onSaved: () -> Unit = {}) {
        // Optimistically add comment to list and bump count on post immediately
        val newComment = Comment(id = "local_${System.currentTimeMillis()}", content = content, authorName = "You", createdAt = "")
        _uiState.update { state ->
            state.copy(
                comments = state.comments + newComment,
                posts = state.posts.map { post ->
                    if (post.id == thoughtId) post.copy(commentCount = post.commentCount + 1) else post
                },
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isPostingComment = true) }
            when (val r = repo.postComment(thoughtId, content)) {
                is Resource.Success -> {
                    _uiState.update {
                        it.copy(
                            isPostingComment = false,
                            commentError = null,
                            comments = it.comments.filterNot { comment -> comment.id == newComment.id },
                        )
                    }
                    onSaved()
                    loadComments(thoughtId)
                }
                is Resource.Error   -> {
                    // Rollback optimistic comment and count on failure
                    _uiState.update { state ->
                        state.copy(
                            isPostingComment = false,
                            commentError = r.message.ifBlank { "Could not post comment. Try again." },
                            comments = state.comments.filter { it.id != newComment.id },
                            posts = state.posts.map { post ->
                                if (post.id == thoughtId) post.copy(commentCount = maxOf(0, post.commentCount - 1)) else post
                            },
                        )
                    }
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadSandeshComments(sandeshId: String, loadMore: Boolean = false) {
        val state = _uiState.value
        if (loadMore && (!state.hasMoreSandeshComments || state.isLoadingMoreSandeshComments)) return
        val page = if (loadMore) state.sandeshCommentsPage + 1 else 1
        viewModelScope.launch {
            if (loadMore) {
                _uiState.update { it.copy(isLoadingMoreSandeshComments = true) }
            } else {
                _uiState.update { it.copy(isLoadingSandeshComments = true, sandeshComments = emptyList(), sandeshCommentsPage = 1) }
            }
            when (val r = repo.getSandeshComments(sandeshId, page)) {
                is Resource.Success -> _uiState.update { s ->
                    s.copy(
                        isLoadingSandeshComments = false,
                        isLoadingMoreSandeshComments = false,
                        sandeshComments = if (loadMore) s.sandeshComments + r.data else r.data,
                        sandeshCommentsPage = page,
                        hasMoreSandeshComments = r.data.size >= 20,
                    )
                }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingSandeshComments = false, isLoadingMoreSandeshComments = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun postSandeshComment(sandeshId: String, content: String, onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            when (val result = repo.postSandeshComment(sandeshId, content)) {
                is Resource.Success -> {
                    onSaved()
                    loadSandeshComments(sandeshId)
                }
                is Resource.Error -> _uiState.update {
                    it.copy(userMessage = result.message.ifBlank { "Could not post comment. Try again." })
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun reactSandesh(id: String) {
        val alreadyReacted = _uiState.value.reactedSandeshIds.contains(id)
        viewModelScope.launch { repo.reactSandesh(id) }
        _uiState.update { state ->
            val newReacted = if (alreadyReacted) state.reactedSandeshIds - id else state.reactedSandeshIds + id
            state.copy(
                reactedSandeshIds = newReacted,
                sandeshes = state.sandeshes.map { s ->
                    if (s.id == id) s.copy(reactionCount = if (alreadyReacted) (s.reactionCount - 1).coerceAtLeast(0) else s.reactionCount + 1) else s
                },
            )
        }
    }

    fun createPost(content: String, space: String, isAnonymous: Boolean = false) {
        if (socketManager.isConnected()) {
            _uiState.update { it.copy(isPosting = true, postError = null) }
            socketManager.emitNewThought(content, space, isAnonymous)
        } else {
            _uiState.update { it.copy(postError = "Could not share post. Check your internet and try again.") }
        }
    }

    fun savePost(thoughtId: String) {
        if (thoughtId in _uiState.value.savingPostIds) return
        val alreadySaved = thoughtId in _uiState.value.savedPostIds
        viewModelScope.launch {
            _uiState.update { it.copy(savingPostIds = it.savingPostIds + thoughtId) }
            val result = if (alreadySaved) repo.unsavePost(thoughtId) else repo.savePost(thoughtId)
            when (result) {
                is Resource.Success -> _uiState.update {
                    it.copy(
                        savedPostIds = if (alreadySaved) it.savedPostIds - thoughtId else it.savedPostIds + thoughtId,
                        savedPosts = if (alreadySaved) it.savedPosts.filterNot { post -> post.id == thoughtId } else it.savedPosts,
                        savingPostIds = it.savingPostIds - thoughtId,
                        userMessage = if (alreadySaved) "Removed from saved posts." else "Post saved.",
                    )
                }
                is Resource.Error -> _uiState.update {
                    it.copy(savingPostIds = it.savingPostIds - thoughtId, userMessage = "Could not save. Try again.")
                }
                is Resource.Loading -> Unit
            }
        }
    }

    fun unsavePost(thoughtId: String) {
        savePost(thoughtId)
    }

    fun loadActivity() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingActivity = true) }
            when (val r = repo.getActivity()) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingActivity = false, activity = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingActivity = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSaved = true) }
            // Load all pages so saved tab shows everything
            var page = 1
            val allPosts = mutableListOf<MehfilPost>()
            var loaded = false
            while (true) {
                when (val r = repo.getSavedPosts(page)) {
                    is Resource.Success -> {
                        loaded = true
                        allPosts.addAll(r.data)
                        if (r.data.size < 20) break   // no more pages
                        page++
                    }
                    is Resource.Error -> {
                        _uiState.update { it.copy(isLoadingSaved = false, userMessage = "Could not load saved posts. Try again.") }
                        break
                    }
                    is Resource.Loading -> break
                }
            }
            if (loaded) {
                val savedIds = allPosts.map { it.id }.toSet()
                _uiState.update { it.copy(isLoadingSaved = false, savedPosts = allPosts, savedPostIds = savedIds) }
            }
        }
    }

    fun addSocketPost(post: MehfilPost) {
        _uiState.update { state ->
            // Skip if already exists (e.g. optimistic insert already added it)
            if (state.posts.any { it.id == post.id }) state
            else state.copy(posts = listOf(post) + state.posts)
        }
    }
    fun clearPostSuccess() { _uiState.update { it.copy(postSuccess = false) } }
    fun clearPostError() { _uiState.update { it.copy(postError = null) } }
    fun clearUserMessage() { _uiState.update { it.copy(userMessage = null) } }
    fun sendDmRequest(
        targetUserId: String,
        targetUserName: String = "",
        contextPostId: String = "",
        contextPreview: String = "",
    ) {
        viewModelScope.launch {
            if (targetUserId.isBlank()) {
                _uiState.update { it.copy(dmError = "Cannot connect: user ID is missing") }
                return@launch
            }
            val currentUid = _uiState.value.currentUserId.ifBlank { dataStore.userId.firstOrNull().orEmpty() }
            if (targetUserId == currentUid && currentUid.isNotBlank()) {
                _uiState.update { it.copy(dmError = "You cannot connect with yourself") }
                return@launch
            }

            // Immediately place UI into Waiting state with clean spinner while connecting and checking premium
            _uiState.update {
                it.copy(
                    dmState = DmState.Waiting(targetUserName.ifBlank { "student" }),
                    dmError = null,
                    dmTargetUserId = targetUserId,
                    dmTargetUserName = targetUserName,
                )
            }

            // Check / Wait for Premium access
            val isPremLocally = (dataStore.premiumFeatureMehfilDm.firstOrNull() ?: false) || (dataStore.isPremium.firstOrNull() ?: false)
            if (!isPremLocally && _uiState.value.isLoadingPremiumFeatures) {
                withTimeoutOrNull(2500) {
                    while (_uiState.value.isLoadingPremiumFeatures) {
                        delay(100)
                    }
                }
            }

            val hasAccess = isPremLocally || _uiState.value.mehfilDm || (premiumRepository.cachedStatus.firstOrNull()?.canUseMehfilDm == true)
            if (!hasAccess) {
                _uiState.update { it.copy(showPremiumGate = true, dmError = "Safar Premium is required for Mehfil Connect", dmState = DmState.Idle) }
                return@launch
            }

            // Ensure socket is connected
            if (!socketManager.isConnected()) {
                val token = dataStore.authToken.firstOrNull() ?: ""
                val userId = dataStore.userId.firstOrNull() ?: ""
                val userName = dataStore.userName.firstOrNull() ?: "Safarite"
                val avatar = dataStore.userAvatar.firstOrNull()
                if (token.isNotEmpty() && userId.isNotEmpty()) {
                    socketManager.connect(token, userId, userName, avatar)
                }
                withTimeoutOrNull(4000) {
                    while (!socketManager.isConnected()) {
                        delay(150)
                    }
                }
            }

            if (!socketManager.isConnected()) {
                _uiState.update { it.copy(dmError = "Mehfil is reconnecting. Please try again.") }
                return@launch
            }

            // Socket is connected and premium verified -> emit request
            _uiState.update { it.copy(dmState = DmState.Waiting(targetUserName.ifBlank { "student" }), dmError = null) }
            socketManager.emitDmRequest(targetUserId, contextPostId, contextPreview)
        }
    }
    fun acceptDm(fromUserId: String) {
        viewModelScope.launch {
            val isPremLocally = (dataStore.premiumFeatureMehfilDm.firstOrNull() ?: false) || (dataStore.isPremium.firstOrNull() ?: false)
            val hasAccess = isPremLocally || _uiState.value.mehfilDm || (premiumRepository.cachedStatus.firstOrNull()?.canUseMehfilDm == true)
            if (!hasAccess) {
                _uiState.update { it.copy(showPremiumGate = true, dmError = null) }
                return@launch
            }
            if (!socketManager.isConnected()) {
                val token = dataStore.authToken.firstOrNull() ?: ""
                val userId = dataStore.userId.firstOrNull() ?: ""
                val userName = dataStore.userName.firstOrNull() ?: "Safarite"
                val avatar = dataStore.userAvatar.firstOrNull()
                if (token.isNotEmpty() && userId.isNotEmpty()) {
                    socketManager.connect(token, userId, userName, avatar)
                }
                withTimeoutOrNull(4000) {
                    while (!socketManager.isConnected()) {
                        delay(150)
                    }
                }
            }
            val pending = _uiState.value.pendingDmRequests.firstOrNull { it.userId == fromUserId }
            val requestId = pending?.requestId.orEmpty()
            socketManager.emitDmAccept(requestId = requestId, fromUserId = fromUserId)
            _uiState.update { it.copy(
                dmError = null,
                dmTargetUserId = fromUserId,
                pendingDmRequests = it.pendingDmRequests.filterNot { p -> p.userId == fromUserId },
            ) }
        }
    }
    fun declineDm(fromUserId: String) {
        val pending = _uiState.value.pendingDmRequests.firstOrNull { it.userId == fromUserId }
        val requestId = pending?.requestId.orEmpty()
        if (!socketManager.isConnected()) {
            _uiState.update { it.copy(dmError = "Could not decline this request. Try again.") }
            return
        }
        socketManager.emitDmDecline(requestId = requestId, fromUserId = fromUserId)
        _uiState.update { it.copy(
            dmState = DmState.Idle,
            pendingDmRequests = it.pendingDmRequests.filter { p -> p.userId != fromUserId },
        ) }
    }
    fun leaveDmRoom() {
        val cur = _uiState.value.dmState
        if (cur is DmState.Open && cur.roomId.isNotBlank()) {
            socketManager.emitDmLeaveRoom(cur.roomId)
        }
        _uiState.update { it.copy(dmState = DmState.Idle) }
    }
    fun sendMessage(message: String) {
        if (!_uiState.value.mehfilDm) {
            _uiState.update { it.copy(showPremiumGate = true, dmError = null) }
            return
        }
        val current = _uiState.value.dmState
        if (current is DmState.Open && current.roomId.isNotBlank() && socketManager.isConnected()) {
            val pending = DmMessage(message, true, senderAvatar = _uiState.value.currentUserAvatar, state = DmMessageState.SENDING)
            socketManager.emitDmMessage(current.roomId, message, pending.id)
            _uiState.update { it.copy(dmState = current.copy(messages = current.messages + pending)) }
        } else {
            _uiState.update { it.copy(dmError = "Chat is reconnecting. Please try again.") }
        }
    }

    private fun loadSandesh() {
        viewModelScope.launch {
            when (val r = repo.getSandesh()) {
                is Resource.Success -> _uiState.update { it.copy(latestSandesh = r.data.first, sandeshes = r.data.second) }
                else -> Unit
            }
        }
    }

}
