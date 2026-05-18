package com.safar.app.ui.mehfil

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.domain.model.MehfilPost
import com.safar.app.ui.butterfly.ButterflyTourState
import com.safar.app.ui.navigation.Routes
import com.safar.app.ui.tour.TourManager
import com.safar.app.ui.tour.mehfilTourSteps

@Composable
fun MehfilScreen(
    currentRoute: String = Routes.MEHFIL,
    isDarkTheme: Boolean = false,
    onNavigate: (String) -> Unit = {},
    onToggleDarkTheme: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    viewModel: MehfilViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableStateOf(MehfilTab.COMMUNITY) }
    var showSandeshSheet by remember { mutableStateOf(false) }
    var sandeshTargetId by remember { mutableStateOf<String?>(null) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showGuidelinesSheet by remember { mutableStateOf(false) }
    var tourState by remember { mutableStateOf<ButterflyTourState?>(null) }
    var commentPost by remember { mutableStateOf<MehfilPost?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val dmChatNavigated = remember { mutableStateOf(false) }

    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) {
            Toast.makeText(context, "Post shared!", Toast.LENGTH_SHORT).show()
            viewModel.clearPostSuccess()
        }
    }

    LaunchedEffect(uiState.postError) {
        uiState.postError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(uiState.dmState) {
        if (uiState.dmState is DmState.Waiting) {
            Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.dmError) {
        uiState.dmError?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(uiState.dmState is DmState.Open) {
        if (uiState.dmState is DmState.Open && !dmChatNavigated.value) {
            dmChatNavigated.value = true
            onNavigate(Routes.DM_CHAT)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.pauseSocket()
                Lifecycle.Event.ON_START -> viewModel.resumeSocket()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            MehfilTab.ANALYTICS -> viewModel.loadActivity()
            MehfilTab.SAVED -> viewModel.loadSavedPosts()
            MehfilTab.COMMUNITY -> viewModel.loadPosts(refresh = true)
            MehfilTab.CONNECTIONS -> Unit
        }
    }

    if (showSandeshSheet && uiState.sandeshes.isNotEmpty()) {
        SandeshBottomSheet(
            sandesh = uiState.latestSandesh ?: uiState.sandeshes.first(),
            sandeshes = uiState.sandeshes,
            reactedSandeshIds = uiState.reactedSandeshIds,
            sandeshComments = uiState.sandeshComments,
            isLoadingSandeshComments = uiState.isLoadingSandeshComments,
            isLoadingMoreSandeshComments = uiState.isLoadingMoreSandeshComments,
            hasMoreSandeshComments = uiState.hasMoreSandeshComments,
            initialCommentTargetId = sandeshTargetId,
            onReact = viewModel::reactSandesh,
            onLoadComments = viewModel::loadSandeshComments,
            onLoadMoreComments = { viewModel.loadSandeshComments(it, loadMore = true) },
            onPostComment = viewModel::postSandeshComment,
            onDismiss = {
                showSandeshSheet = false
                sandeshTargetId = null
            },
        )
    }

    commentPost?.let { post ->
        CommentsBottomSheet(
            post = post,
            comments = uiState.comments,
            isLoading = uiState.isLoadingComments,
            isLoadingMore = uiState.isLoadingMoreComments,
            hasMore = uiState.hasMoreComments,
            isPosting = uiState.isPostingComment,
            commentInput = commentInput,
            onCommentChange = { commentInput = it },
            onPost = {
                viewModel.postComment(post.id, commentInput)
                commentInput = ""
            },
            onLoadMore = { viewModel.loadComments(uiState.currentCommentPostId, loadMore = true) },
            onDismiss = {
                commentPost = null
                commentInput = ""
            },
        )
    }

    if (showCreatePostSheet) {
        CreatePostSheet(
            selectedSpace = uiState.selectedSpace,
            onPost = { content, space, isAnonymous ->
                viewModel.createPost(content, space, isAnonymous)
                showCreatePostSheet = false
            },
            onDismiss = { showCreatePostSheet = false },
        )
    }

    if (showGuidelinesSheet) {
        GuidelinesSheet(onDismiss = { showGuidelinesSheet = false })
    }

    MehfilContent(
        uiState = uiState,
        selectedTab = selectedTab,
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        searchActive = searchActive,
        searchQuery = searchQuery,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        onTabSelected = { selectedTab = it },
        onSearchActiveChange = {
            searchActive = it
            if (!it) searchQuery = ""
        },
        onSearchQueryChange = { searchQuery = it },
        onClearSearch = { searchQuery = "" },
        onTourClick = { tourState?.start() },
        onGuidelinesClick = { showGuidelinesSheet = true },
        onCreatePostClick = { showCreatePostSheet = true },
        onLoadPosts = viewModel::loadPosts,
        onJoinRoom = viewModel::joinRoom,
        onReactSandesh = viewModel::reactSandesh,
        onLikePost = viewModel::toggleLike,
        onSavePost = viewModel::savePost,
        onUnsavePost = viewModel::unsavePost,
        onCommentClick = { post ->
            commentPost = post
            viewModel.loadComments(post.id)
        },
        onSandeshCommentClick = { id ->
            sandeshTargetId = id
            showSandeshSheet = true
        },
        onConnect = { post ->
            dmChatNavigated.value = false
            viewModel.sendDmRequest(
                targetUserId = post.userId,
                targetUserName = post.authorName,
                contextPostId = post.id,
                contextPreview = post.content.take(60),
            )
            selectedTab = MehfilTab.CONNECTIONS
        },
        onAcceptDm = { userId ->
            dmChatNavigated.value = false
            viewModel.acceptDm(userId)
            onNavigate(Routes.DM_CHAT)
        },
        onDeclineDm = viewModel::declineDm,
        onOpenDmChat = { onNavigate(Routes.DM_CHAT) },
    )

    TourManager(
        dataStore = viewModel.dataStore,
        steps = mehfilTourSteps,
        askOnFirstVisit = false,
        onTourStateReady = { tourState = it },
    )
}
