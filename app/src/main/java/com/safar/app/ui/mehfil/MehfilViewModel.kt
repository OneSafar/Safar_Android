package com.safar.app.ui.mehfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safar.app.data.local.SafarDataStore
import com.safar.app.data.remote.socket.MehfilSocketManager
import com.safar.app.data.remote.socket.toDomain
import com.safar.app.domain.model.*
import com.safar.app.domain.repository.MehfilRepository
import com.safar.app.domain.repository.AuthRepository
import com.safar.app.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DmMessage(val text: String, val isMine: Boolean)

sealed class DmState {
    object Idle : DmState()
    object Waiting : DmState()
    data class IncomingRequest(val fromUserId: String, val fromUserName: String) : DmState()
    data class Open(val peerId: String, val peerName: String, val roomId: String, val messages: List<DmMessage> = emptyList()) : DmState()
}

data class PendingDmRequest(val userId: String, val userName: String, val requestId: String)

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
    val dmState: DmState = DmState.Idle,
    val dmError: String? = null,
    val dmTargetUserId: String? = null,
    val dmTargetUserName: String = "",
    val socketConnected: Boolean = false,
    val pendingDmRequests: List<PendingDmRequest> = emptyList(),
    val currentUserId: String = "",
    val dmRequestId: String = "",
    // Local overrides so optimistic like state survives list refreshes
    val localLikeOverrides: Map<String, Boolean> = emptyMap(),
    val localReactionOverrides: Map<String, Int> = emptyMap(),
    val reactedSandeshIds: Set<String> = emptySet(),
)

@HiltViewModel
class MehfilViewModel @Inject constructor(
    private val repo: MehfilRepository,
    private val socketManager: MehfilSocketManager,
    val dataStore: SafarDataStore,
    private val authRepo: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MehfilUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSandesh()
        initSocket()
    }

    private fun initSocket() {
        viewModelScope.launch {
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

            _uiState.update { it.copy(isLoadingPosts = true, currentUserId = userId) }

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
                    if (!connected && _uiState.value.posts.isEmpty()) {
                        loadPostsFallback()
                    }
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
                        "request_sent"     -> _uiState.update { it.copy(dmRequestId = event.message) }
                        "incoming_request" -> _uiState.update { it.copy(
                            dmState = DmState.IncomingRequest(event.fromUserId, event.fromUserName),
                            pendingDmRequests = (it.pendingDmRequests + PendingDmRequest(event.fromUserId, event.fromUserName, event.requestId)).distinctBy { p -> p.userId },
                        ) }
                        "opened"           -> _uiState.update { it.copy(
                            dmState = DmState.Open(
                                peerId = event.fromUserId,
                                peerName = event.fromUserName.ifBlank { it.dmTargetUserName }.ifBlank { event.fromUserId },
                                roomId = event.roomId,
                            ),
                        ) }
                        "accepted"         -> _uiState.update { it.copy(
                            dmState = DmState.Open(
                                peerId = event.fromUserId,
                                peerName = event.fromUserName.ifBlank { it.dmTargetUserName }.ifBlank { event.fromUserId },
                                roomId = event.roomId,
                            ),
                        ) }
                        "declined"         -> _uiState.update { it.copy(dmState = DmState.Idle, dmError = "Request declined") }
                        "sync_pending"     -> _uiState.update { it.copy(pendingDmRequests = event.pendingList.map { id -> PendingDmRequest(id, id, "") }) }
                        "message"          -> {
                            val cur = _uiState.value.dmState
                            if (cur is DmState.Open) {
                                // Skip server echo of our own message (no fromUserId or fromUserId == currentUserId)
                                val isEcho = event.fromUserId.isBlank() || event.fromUserId == _uiState.value.currentUserId
                                if (!isEcho) {
                                    val updatedPeerName = if (cur.peerName.isBlank() || cur.peerName == cur.peerId) event.fromUserName.ifBlank { cur.peerName } else cur.peerName
                                    _uiState.update { it.copy(dmState = cur.copy(peerName = updatedPeerName, messages = cur.messages + DmMessage(event.message, isMine = false))) }
                                }
                            }
                        }
                        "error" -> _uiState.update { it.copy(dmError = event.message) }
                    }
                }
            }
        }
    }

    private fun loadPostsFallback() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPosts = true) }
            when (val r = repo.getSavedPosts(1)) {
                is Resource.Success -> _uiState.update { it.copy(isLoadingPosts = false, posts = r.data) }
                is Resource.Error   -> _uiState.update { it.copy(isLoadingPosts = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun joinRoom(room: String) {
        _uiState.update { it.copy(selectedSpace = room, posts = emptyList(), currentPage = 1, isLoadingPosts = true) }
        if (socketManager.isConnected()) {
            socketManager.joinRoomAndLoad(room)
        } else {
            viewModelScope.launch {
                val token = dataStore.authToken.first() ?: return@launch
                var userId = dataStore.userId.first()
                if (userId.isNullOrBlank()) {
                    authRepo.getMe()
                    userId = dataStore.userId.first()
                }
                if (userId.isNullOrBlank()) return@launch
                val userName = dataStore.userName.first() ?: "Safarite"
                val avatar   = dataStore.userAvatar.first()
                socketManager.connect(token = token, userId = userId, userName = userName, userAvatar = avatar, initialRoom = room)
            }
        }
    }

    fun loadPosts(refresh: Boolean = false) {
        if (refresh) {
            _uiState.update { it.copy(isLoadingPosts = true, posts = emptyList(), currentPage = 1) }
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
            state.copy(posts = merged, currentPage = page, totalPages = if (hasMore) page + 1 else page, hasMore = hasMore, isLoadingPosts = false)
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
                is Resource.Error   -> _uiState.update { it.copy(isLoadingComments = false, isLoadingMoreComments = false) }
                is Resource.Loading -> Unit
            }
        }
    }

    fun postComment(thoughtId: String, content: String) {
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
                is Resource.Success -> _uiState.update { it.copy(isPostingComment = false) }
                is Resource.Error   -> {
                    // Rollback optimistic comment and count on failure
                    _uiState.update { state ->
                        state.copy(
                            isPostingComment = false,
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

    fun postSandeshComment(sandeshId: String, content: String) {
        viewModelScope.launch {
            repo.postSandeshComment(sandeshId, content)
            loadSandeshComments(sandeshId)
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
        // Optimistic insert so the poster sees their post immediately
        val optimistic = MehfilPost(
            id = java.util.UUID.randomUUID().toString(),
            content = content,
            space = space,
            authorName = if (isAnonymous) "Anonymous" else "You",
            userId = "",  // empty so connect button is hidden for own posts
            createdAt = "",
            reactionCount = 0,
            commentCount = 0,
            userLiked = false,
        )
        addSocketPost(optimistic)
        if (socketManager.isConnected()) {
            socketManager.emitNewThought(content, space, isAnonymous)
        }
        _uiState.update { it.copy(postSuccess = true) }
    }

    fun savePost(thoughtId: String) {
        val alreadySaved = thoughtId in _uiState.value.savedPostIds
        if (alreadySaved) {
            _uiState.update { it.copy(savedPostIds = it.savedPostIds - thoughtId) }
            viewModelScope.launch { repo.unsavePost(thoughtId) }
        } else {
            _uiState.update { it.copy(savedPostIds = it.savedPostIds + thoughtId) }
            viewModelScope.launch { repo.savePost(thoughtId) }
        }
    }

    fun unsavePost(thoughtId: String) {
        _uiState.update { state ->
            state.copy(
                savedPostIds = state.savedPostIds - thoughtId,
                savedPosts   = state.savedPosts.filter { it.id != thoughtId },
            )
        }
        viewModelScope.launch { repo.unsavePost(thoughtId) }
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
            _uiState.update { it.copy(isLoadingSaved = true, savedPosts = emptyList()) }
            // Load all pages so saved tab shows everything
            var page = 1
            val allPosts = mutableListOf<MehfilPost>()
            while (true) {
                when (val r = repo.getSavedPosts(page)) {
                    is Resource.Success -> {
                        allPosts.addAll(r.data)
                        if (r.data.size < 20) break   // no more pages
                        page++
                    }
                    else -> break
                }
            }
            val savedIds = allPosts.map { it.id }.toSet()
            _uiState.update { it.copy(isLoadingSaved = false, savedPosts = allPosts, savedPostIds = it.savedPostIds + savedIds) }
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
    fun sendDmRequest(targetUserId: String, targetUserName: String = "", contextPostId: String = "", contextPreview: String = "") {
        if (targetUserId.isBlank()) {
            _uiState.update { it.copy(dmError = "Cannot connect: user ID is missing") }
            return
        }
        if (targetUserId == _uiState.value.currentUserId) {
            _uiState.update { it.copy(dmError = "You cannot connect with yourself") }
            return
        }
        _uiState.update { it.copy(dmState = DmState.Waiting, dmError = null, dmTargetUserId = targetUserId, dmTargetUserName = targetUserName) }
        val s = socketManager
        if (s.isConnected()) {
            s.emitDmRequest(targetUserId, contextPostId, contextPreview)
        }
    }
    fun acceptDm(fromUserId: String) {
        val pending = _uiState.value.pendingDmRequests.firstOrNull { it.userId == fromUserId }
        val requestId = pending?.requestId ?: ""
        val peerName  = pending?.userName ?: fromUserId
        socketManager.emitDmAccept(requestId)
        _uiState.update { it.copy(
            dmState = DmState.Open(peerId = fromUserId, peerName = peerName, roomId = ""),
            pendingDmRequests = it.pendingDmRequests.filter { p -> p.userId != fromUserId },
        ) }
    }
    fun declineDm(fromUserId: String) {
        socketManager.emitDmDecline(fromUserId)
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
        val current = _uiState.value.dmState
        if (current is DmState.Open) {
            socketManager.emitDmMessage(current.roomId, message)
            _uiState.update { it.copy(dmState = current.copy(messages = current.messages + DmMessage(message, true))) }
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

    /** Called when the app goes to background (ON_STOP). Disconnects the socket. */
    fun pauseSocket() {
        socketManager.disconnect()
    }

    /** Called when the app returns to foreground (ON_START). Reconnects if not already connected. */
    fun resumeSocket() {
        if (!socketManager.isConnected()) {
            initSocket()
        }
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }
}
