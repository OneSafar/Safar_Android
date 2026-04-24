package com.safar.app.ui.mehfil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.safar.app.ui.theme.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safar.app.domain.model.*
import com.safar.app.ui.drawer.SafarDrawerScaffold
import com.safar.app.ui.navigation.Routes
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.rememberUpdatedState

private enum class MehfilTab(val label: String, val icon: ImageVector) {
    COMMUNITY  ("Community",   Icons.Default.Groups),
    SAVED      ("Saved",       Icons.Default.Bookmark),
    ANALYTICS  ("Activity",    Icons.Default.BarChart),
    CONNECTIONS("Connections", Icons.Default.PersonAdd),
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedTab by remember { mutableStateOf(MehfilTab.COMMUNITY) }
    var showSandeshSheet by remember { mutableStateOf(false) }
    var showCreatePostSheet by remember { mutableStateOf(false) }
    var showGuidelinesSheet by remember { mutableStateOf(false) }
    var tourState by remember { mutableStateOf<com.safar.app.ui.butterfly.ButterflyTourState?>(null) }
    var commentPost by remember { mutableStateOf<MehfilPost?>(null) }
    var commentInput by remember { mutableStateOf("") }
    // Search state — lives at screen level so topbar can control it
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) {
            Toast.makeText(context, "Post shared!", Toast.LENGTH_SHORT).show()
            viewModel.clearPostSuccess()
        }
    }

    LaunchedEffect(uiState.postError) {
        if (uiState.postError != null) {
            Toast.makeText(context, uiState.postError, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.dmState) {
        if (uiState.dmState is DmState.Waiting) {
            Toast.makeText(context, "Connection request sent!", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(uiState.dmError) {
        if (uiState.dmError != null) {
            Toast.makeText(context, uiState.dmError, Toast.LENGTH_SHORT).show()
        }
    }

    // Use a ref so it survives recompositions without triggering them
    val dmChatNavigated = remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(uiState.dmState is DmState.Open) {
        if (uiState.dmState is DmState.Open && !dmChatNavigated.value) {
            dmChatNavigated.value = true
            onNavigate(Routes.DM_CHAT)
        }
    }

    // ── P2P socket lifecycle: only pause when app goes to background (ON_STOP),
    //    NOT on inner back-navigation or route changes within the app.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP  -> viewModel.pauseSocket()
                Lifecycle.Event.ON_START -> viewModel.resumeSocket()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            MehfilTab.ANALYTICS   -> viewModel.loadActivity()
            MehfilTab.SAVED       -> viewModel.loadSavedPosts()
            MehfilTab.COMMUNITY   -> viewModel.loadPosts(refresh = true)
            else -> Unit
        }
    }

    if (showSandeshSheet && uiState.latestSandesh != null) {
        SandeshBottomSheet(
            sandesh = uiState.latestSandesh!!,
            sandeshes = uiState.sandeshes,
            reactedSandeshIds = uiState.reactedSandeshIds,
            sandeshComments = uiState.sandeshComments,
            isLoadingSandeshComments = uiState.isLoadingSandeshComments,
            isLoadingMoreSandeshComments = uiState.isLoadingMoreSandeshComments,
            hasMoreSandeshComments = uiState.hasMoreSandeshComments,
            onReact = { viewModel.reactSandesh(it) },
            onLoadComments = { viewModel.loadSandeshComments(it) },
            onLoadMoreComments = { viewModel.loadSandeshComments(it, loadMore = true) },
            onPostComment = { id, c -> viewModel.postSandeshComment(id, c) },
            onDismiss = { showSandeshSheet = false },
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
            onPost = { viewModel.postComment(post.id, commentInput); commentInput = "" },
            onLoadMore = { viewModel.loadComments(uiState.currentCommentPostId, loadMore = true) },
            onDismiss = { commentPost = null; commentInput = "" },
        )
    }

    if (showCreatePostSheet) {
        CreatePostSheet(
            selectedSpace = uiState.selectedSpace,
            onPost = { content, space, anon ->
                viewModel.createPost(content, space, anon)
                showCreatePostSheet = false
            },
            onDismiss = { showCreatePostSheet = false },
        )
    }

    if (showGuidelinesSheet) {
        GuidelinesSheet(onDismiss = { showGuidelinesSheet = false })
    }

    SafarDrawerScaffold(
        title = "Mehfil",
        subtitle = "Safar",
        currentRoute = currentRoute,
        isDarkTheme = isDarkTheme,
        onNavigate = onNavigate,
        onToggleDarkTheme = onToggleDarkTheme,
        onLanguageClick = onLanguageClick,
        topBarActions = {
            IconButton(onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" }) {
                Icon(if (searchActive) Icons.Default.Close else Icons.Default.Search, contentDescription = "Search")
            }
            IconButton(onClick = { tourState?.start() }) {
                Icon(Icons.Default.HelpOutline, contentDescription = "Guide")
            }
            IconButton(onClick = { showGuidelinesSheet = true }) {
                Icon(Icons.Default.Info, contentDescription = "Guidelines")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                    val pendingCount = uiState.pendingDmRequests.size
                    MehfilTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = {
                                if (tab == MehfilTab.CONNECTIONS && pendingCount > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text(if (pendingCount > 9) "9+" else pendingCount.toString(), fontSize = 9.sp) }
                                    }) {
                                        Icon(tab.icon, null)
                                    }
                                } else {
                                    Icon(tab.icon, null)
                                }
                            },
                            label = { Text(tab.label, fontWeight = if (selectedTab == tab) FontWeight.SemiBold else FontWeight.Normal, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == MehfilTab.COMMUNITY) {
                    FloatingActionButton(
                        onClick = { showCreatePostSheet = true },
                        containerColor = when (uiState.selectedSpace) {
                            "ACADEMIC"   -> Blue500
                            "REFLECTIVE" -> Violet500
                            else         -> MaterialTheme.colorScheme.primary
                        },
                        shape = CircleShape,
                    ) { Icon(Icons.Default.Add, null, tint = Color.White) }
                }
            },
        ) { innerPadding ->
            if (uiState.isInitializing) {
                Box(
                    Modifier.fillMaxSize()
                        .padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Setting up Mehfil...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                return@Scaffold
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = padding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding()),
            ) {
                // Search bar — expands below topbar when search icon tapped
                AnimatedVisibility(
                    visible = searchActive && selectedTab == MehfilTab.COMMUNITY,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    LaunchedEffect(searchActive) {
                        if (searchActive) { kotlinx.coroutines.delay(80); searchFocusRequester.requestFocus() }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search posts or names…", fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        )
                    }
                }

                when (selectedTab) {
                    MehfilTab.COMMUNITY   -> CommunityTab(
                        uiState = uiState,
                        viewModel = viewModel,
                        searchQuery = searchQuery,
                        onClearSearch = { searchQuery = "" },
                        onSandeshClick = { showSandeshSheet = true },
                        onCommentClick = { post -> commentPost = post; viewModel.loadComments(post.id) },
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
                    )
                    MehfilTab.SAVED       -> SavedTab(uiState, viewModel, onCommentClick = { post -> commentPost = post; viewModel.loadComments(post.id) })
                    MehfilTab.ANALYTICS   -> AnalyticsTab(uiState)
                    MehfilTab.CONNECTIONS -> ConnectionsTab(uiState, viewModel, onNavigateToDmChat = { onNavigate(Routes.DM_CHAT) }, onAcceptDm = { userId ->
                            dmChatNavigated.value = false
                            viewModel.acceptDm(userId)
                            onNavigate(Routes.DM_CHAT)
                        })
                }
            }
        }

        // Tour overlay (no ask dialog — already offered on Nishtha)
        com.safar.app.ui.tour.TourManager(
            dataStore = viewModel.dataStore,
            steps = com.safar.app.ui.tour.mehfilTourSteps,
            askOnFirstVisit = false,
            onTourStateReady = { tourState = it },
        )
        } // end outer Box
    }
}

@Composable
private fun SandeshHeaderCard(sandesh: Sandesh, reacted: Boolean, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                Text("📢", fontSize = 16.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("SANDESH", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                Text(sandesh.content, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(
                        if (reacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = if (reacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("${sandesh.reactionCount}", fontSize = 10.sp, color = if (reacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${sandesh.commentCount}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun CommunityTab(
    uiState: MehfilUiState,
    viewModel: MehfilViewModel,
    searchQuery: String = "",
    onClearSearch: () -> Unit = {},
    onSandeshClick: () -> Unit,
    onCommentClick: (MehfilPost) -> Unit,
    onConnect: (MehfilPost) -> Unit,
) {
    val listState = rememberLazyListState()

    // Filter posts by search query — client-side on already-loaded posts
    val filteredPosts = remember(uiState.posts, searchQuery) {
        if (searchQuery.isBlank()) uiState.posts
        else uiState.posts.filter { post ->
            post.content.contains(searchQuery, ignoreCase = true) ||
            post.authorName.contains(searchQuery, ignoreCase = true)
        }
    }

    // Hide sandesh when scrolling down (first item scrolled past), show when at top
    val showSandesh by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 80 }
    }

    // Scroll to top when room/tab changes
    LaunchedEffect(uiState.selectedSpace) {
        listState.scrollToItem(0)
    }

    val hasMore by rememberUpdatedState(uiState.hasMore)
    val isLoadingPosts by rememberUpdatedState(uiState.isLoadingPosts)

    // Pagination — fires when user scrolls within 4 items of the bottom
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4
        }.collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingPosts) {
                viewModel.loadPosts(refresh = false)
            }
        }
    }

    // After a new page arrives the viewport stays anchored to the same item —
    // meaning lastVisible is now far from the new total, so snapshotFlow won't
    // re-fire on its own. Re-check manually: if the user was already at the
    // bottom of the OLD list, load the next page immediately.
    LaunchedEffect(uiState.posts.size) {
        if (hasMore && !isLoadingPosts) {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            // Were they near the bottom before this page arrived?
            // Use the previous total (total - 15 approx) as reference — simpler:
            // just check if lastVisible is within the last quarter of the list
            val nearBottom = total > 0 && lastVisible >= (total * 0.75f).toInt()
            val searchNeedsMore = searchQuery.isNotBlank() && filteredPosts.size < 8
            if (nearBottom || searchNeedsMore) {
                viewModel.loadPosts(refresh = false)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // ── Header row ──────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val resultText = if (searchQuery.isBlank()) "Community Space"
                             else "${filteredPosts.size} result${if (filteredPosts.size != 1) "s" else ""} for \"$searchQuery\""
            Text(
                resultText,
                fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f),
                color = if (searchQuery.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
            if (uiState.onlineCount > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(Green500))
                    Text("${uiState.onlineCount} online", fontSize = 11.sp, color = Green500)
                }
            } else if (!uiState.socketConnected) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Connecting...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }



        // Sandesh — hides on scroll down, reappears at top
        androidx.compose.animation.AnimatedVisibility(
            visible = showSandesh && uiState.latestSandesh != null,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
        ) {
            uiState.latestSandesh?.let { SandeshHeaderCard(sandesh = it, reacted = uiState.reactedSandeshIds.contains(it.id), onClick = onSandeshClick) }
        }

        Text(
            "The student lounge for unfiltered thoughts and academic life-hacks.",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp),
        )
        Spacer(Modifier.height(6.dp))

        // Room tabs — pill-style segmented row
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            val rooms = listOf(
                Triple("ALL",        "All",           Rose900),
                Triple("ACADEMIC",   "Academic Hall", Teal600),
                Triple("REFLECTIVE", "Thoughts",      Violet600),
            )
            rooms.forEach { (room, label, color) ->
                val selected = uiState.selectedSpace == room
                Box(
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(50.dp))
                        .background(if (selected) color else Color.Transparent)
                        .clickable { viewModel.joinRoom(room) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else color,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))

        if (uiState.isLoadingPosts && uiState.posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Column
        }
        if (!uiState.isLoadingPosts && uiState.posts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💬", fontSize = 48.sp)
                    Text("No posts yet. Be the first!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return@Column
        }
        if (searchQuery.isNotBlank() && filteredPosts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.isLoadingPosts && uiState.hasMore) {
                        // Still fetching pages — show spinner, not "no results"
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        Text("Searching posts…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    } else {
                        Text("🔍", fontSize = 40.sp)
                        Text("No results for \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        TextButton(onClick = { onClearSearch() }) { Text("Clear search") }
                    }
                }
            }
            return@Column
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(filteredPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    isSaved = post.id in uiState.savedPostIds,
                    onLike = { viewModel.toggleLike(post) },
                    onComment = { onCommentClick(post) },
                    onSave = { viewModel.savePost(post.id) },
                    onConnect = if (post.userId.isNotEmpty() && post.userId != uiState.currentUserId)
                        { { onConnect(post) } } else null,
                )
            }
            if (uiState.isLoadingPosts && uiState.posts.isNotEmpty()) {
                item(key = "__loading__") {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                            if (searchQuery.isNotBlank()) {
                                Text("Searching more pages…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            // When search active and no more pages, show end-of-results note
            if (searchQuery.isNotBlank() && !uiState.hasMore && filteredPosts.isNotEmpty()) {
                item(key = "__end__") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("All matching posts shown", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(post: MehfilPost, isSaved: Boolean, onLike: () -> Unit, onComment: () -> Unit, onSave: () -> Unit, onConnect: (() -> Unit)? = null) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Text(post.authorName.firstOrNull()?.uppercase() ?: "A", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(post.authorName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(formatPostDate(post.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (post.space.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(spaceColor(post.space).copy(0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(post.space, fontSize = 9.sp, color = spaceColor(post.space), fontWeight = FontWeight.Bold)
                    }
                }
                // Connect button — hidden for own posts (onConnect is null) and anonymous posts (userId empty)
                if (onConnect != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(0.12f))
                            .clickable(onClick = onConnect),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Connect", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(post.content, fontSize = 14.sp, lineHeight = 20.sp)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.08f))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clickable(onClick = onLike)) {
                    Icon(if (post.userLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, modifier = Modifier.size(17.dp), tint = if (post.userLiked) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${post.reactionCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clickable(onClick = onComment)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${post.commentCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    null,
                    modifier = Modifier.size(17.dp).clickable(onClick = onSave),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun spaceColor(space: String): Color = when (space.uppercase()) {
    "ACADEMIC"   -> Blue500
    "REFLECTIVE" -> Violet500
    else         -> MaterialTheme.colorScheme.primary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    post: MehfilPost,
    comments: List<Comment>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isPosting: Boolean,
    commentInput: String,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${comments.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
            } else if (comments.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text("No comments yet. Be the first!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) }
            } else {
                val commentsListState = rememberLazyListState()
                LaunchedEffect(commentsListState) {
                    snapshotFlow {
                        val info = commentsListState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                        last >= info.totalItemsCount - 2
                    }.collect { nearEnd -> if (nearEnd && hasMore && !isLoadingMore) onLoadMore() }
                }
                LazyColumn(state = commentsListState, modifier = Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(comments) { comment ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)), contentAlignment = Alignment.Center) {
                                Text(comment.authorName.firstOrNull()?.uppercase() ?: "A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(comment.authorName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(comment.content, fontSize = 13.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                    if (isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = commentInput, onValueChange = onCommentChange, placeholder = { Text("Add a comment...", fontSize = 13.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(24.dp))
                IconButton(onClick = { if (commentInput.isNotBlank()) onPost() }, modifier = Modifier.size(44.dp).clip(CircleShape).background(if (commentInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f))) {
                    if (isPosting) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    else Icon(Icons.Default.Send, null, tint = if (commentInput.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SandeshBottomSheet(
    sandesh: Sandesh,
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String> = emptySet(),
    sandeshComments: List<Comment>,
    isLoadingSandeshComments: Boolean = false,
    isLoadingMoreSandeshComments: Boolean = false,
    hasMoreSandeshComments: Boolean = false,
    onReact: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onLoadMoreComments: (String) -> Unit,
    onPostComment: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var commentTargetId by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf("") }
    val showComments = commentTargetId != null

    LaunchedEffect(commentTargetId) { commentTargetId?.let { onLoadComments(it) } }

    ModalBottomSheet(
        onDismissRequest = { if (showComments) commentTargetId = null else onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        androidx.compose.animation.AnimatedContent(targetState = showComments, label = "sandesh_nav") { inComments ->
            if (inComments) {
                Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        IconButton(onClick = { commentTargetId = null; commentText = "" }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        if (!isLoadingSandeshComments) Text("${sandeshComments.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                    if (isLoadingSandeshComments) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        val sandeshListState = rememberLazyListState()
                        LaunchedEffect(sandeshListState) {
                            snapshotFlow {
                                val info = sandeshListState.layoutInfo
                                val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                                last >= info.totalItemsCount - 2
                            }.collect { nearEnd ->
                                if (nearEnd && hasMoreSandeshComments && !isLoadingMoreSandeshComments)
                                    commentTargetId?.let { onLoadMoreComments(it) }
                            }
                        }
                        LazyColumn(
                            state = sandeshListState,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (sandeshComments.isEmpty()) {
                                item { Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) { Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp) } }
                            } else {
                                items(sandeshComments) { c ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)), contentAlignment = Alignment.Center) {
                                            Text(c.authorName.firstOrNull()?.uppercase() ?: "A", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(c.authorName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                            Text(c.content, fontSize = 13.sp, lineHeight = 18.sp)
                                        }
                                    }
                                }
                            }
                            if (isLoadingMoreSandeshComments) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = commentText, onValueChange = { commentText = it }, placeholder = { Text("Add a comment...", fontSize = 13.sp) }, modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(24.dp))
                        IconButton(
                            onClick = {
                                val id = commentTargetId
                                if (commentText.isNotBlank() && id != null) { onPostComment(id, commentText); commentText = "" }
                            },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f)),
                        ) {
                            Icon(Icons.Default.Send, null, tint = if (commentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sandesh", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Messages from the community", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    sandeshes.forEach { s ->
                        val isReacted = reactedSandeshIds.contains(s.id)
                        Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(s.content, fontSize = 14.sp, lineHeight = 20.sp)
                                Text(s.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                    Row(Modifier.clickable { onReact(s.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(
                                            if (isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            null,
                                            modifier = Modifier.size(15.dp),
                                            tint = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text("${s.reactionCount}", fontSize = 12.sp, color = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(Modifier.clickable { commentTargetId = s.id }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Icon(Icons.Default.ChatBubbleOutline, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                        Text("${s.commentCount} comments", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(selectedSpace: String, onPost: (String, String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var space by remember { mutableStateOf(if (selectedSpace == "ALL") "REFLECTIVE" else selectedSpace) }
    var isAnonymous by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New Post", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(value = content, onValueChange = { content = it }, placeholder = { Text("What's on your mind?") }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), minLines = 4, shape = RoundedCornerShape(12.dp))
            Text("Space", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("REFLECTIVE", "ACADEMIC").forEach { s ->
                    FilterChip(selected = space == s, onClick = { space = s }, label = { Text(s.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) })
                }
            }
            // Anonymous toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (isAnonymous) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, if (isAnonymous) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("👻", fontSize = 20.sp)
                    Column(Modifier.weight(1f)) {
                        Text("Post Anonymously", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Your name won't be shown", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        )
                    )
                }
            }
            Button(onClick = { if (content.isNotBlank()) onPost(content, space, isAnonymous) }, enabled = content.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text(if (isAnonymous) "Post Anonymously" else "Post", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuidelinesSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Community Guidelines", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Posting Rules", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            GuidelineItem("🎓", "Academic Hall", "Research, study hacks, and career help only. No venting.")
            GuidelineItem("💬", "Thoughts", "Emotional support and venting. Move here for personal struggles.")
            Text("Consequences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            GuidelineItem("🚫", "Report-Based Bans", "1+ reports trigger automatic bans (2D → 7D → Permanent).")
            GuidelineItem("👻", "Shadow Banning", "Repeated spam results in silent silencing — others won't see you.")
        }
    }
}

@Composable
private fun GuidelineItem(emoji: String, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(emoji, fontSize = 18.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun SavedTab(uiState: MehfilUiState, viewModel: MehfilViewModel, onCommentClick: (MehfilPost) -> Unit) {
    if (uiState.isLoadingSaved) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }
    if (uiState.savedPosts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔖", fontSize = 48.sp)
                Text("No saved posts yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Saved Posts", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${uiState.savedPosts.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(uiState.savedPosts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    isSaved = true,
                    onLike = { viewModel.toggleLike(post) },
                    onComment = { onCommentClick(post) },
                    onSave = { viewModel.unsavePost(post.id) },
                )
            }
        }
    }
}

@Composable
private fun AnalyticsTab(uiState: MehfilUiState) {
    if (uiState.isLoadingActivity) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text("My Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        val posts     = uiState.activity.count { it.type == "post" }
        val comments  = uiState.activity.count { it.type == "comment" }
        val likes     = uiState.activity.count { it.type == "like" }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityStatCard("Posts", "$posts", Icons.Default.Article, Modifier.weight(1f))
            ActivityStatCard("Comments", "$comments", Icons.Default.ChatBubble, Modifier.weight(1f))
            ActivityStatCard("Likes", "$likes", Icons.Default.Favorite, Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        if (uiState.activity.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No activity yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.activity) { item ->
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(when (item.type) { "post" -> "✍️"; "comment" -> "💬"; "like" -> "❤️"; else -> "📌" }, fontSize = 20.sp)
                        Column(Modifier.weight(1f)) {
                            Text(when (item.type) { "post" -> "Posted"; "comment" -> "Commented: ${item.comment ?: ""}"; "like" -> "Liked a post"; else -> item.type }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.thoughtContent.take(60), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(item.createdAt.take(10), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(shape = RoundedCornerShape(12.dp), modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConnectionsTab(uiState: MehfilUiState, viewModel: MehfilViewModel, onNavigateToDmChat: () -> Unit, onAcceptDm: (String) -> Unit) {
    val pending = uiState.pendingDmRequests
    val dmState = uiState.dmState

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Pending requests — compact list
        if (pending.isNotEmpty()) {
            Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.HourglassEmpty, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("Incoming Requests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    pending.forEach { req ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)), contentAlignment = Alignment.Center) {
                                Text(req.userName.firstOrNull()?.uppercase() ?: "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(req.userName, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { onAcceptDm(req.userId) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Text("Accept", fontSize = 12.sp)
                            }
                            TextButton(onClick = { viewModel.declineDm(req.userId) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                                Text("Decline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // DM status
        when (dmState) {
            is DmState.Idle -> {
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Connect with someone", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Tap Connect on any post to start a private chat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                    }
                }
            }
            is DmState.Waiting -> {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                    Text("Waiting for response…", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            is DmState.IncomingRequest -> {
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📨 ${dmState.fromUserName} wants to connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onAcceptDm(dmState.fromUserId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) { Text("Accept", fontSize = 13.sp) }
                            OutlinedButton(onClick = { viewModel.declineDm(dmState.fromUserId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) { Text("Decline", fontSize = 13.sp) }
                        }
                    }
                }
            }
            is DmState.Open -> {
                // Already navigated to DmChatScreen, show a resume card here
                Card(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onNavigateToDmChat() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(0.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Green500))
                        Text("Chat with ${dmState.peerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (uiState.dmError != null) {
            Text(uiState.dmError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
fun DmChatScreen(
    viewModel: MehfilViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dmState = uiState.dmState

    if (dmState !is DmState.Open) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    BackHandler { onBack() }
    LaunchedEffect(dmState.messages.size) {
        if (dmState.messages.isNotEmpty()) listState.animateScrollToItem(dmState.messages.size - 1)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    Box(Modifier.size(34.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                        Text(dmState.peerName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(dmState.peerName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).clip(CircleShape).background(Green500))
                            Text("Ephemeral · Connected", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(
                        onClick = { viewModel.leaveDmRoom(); onBack() },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(0.5f)),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text("Leave", fontSize = 12.sp)
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp) {
                Row(
                    Modifier.navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        placeholder = { Text("Message…", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        textStyle = TextStyle(fontSize = 14.sp),
                    )
                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                viewModel.sendMessage(messageInput.trim())
                                messageInput = ""
                            }
                        },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(
                            if (messageInput.isNotBlank()) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(0.12f)
                        ),
                    ) {
                        Icon(Icons.Default.Send, null, tint = if (messageInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
    ) { innerPadding ->
        if (dmState.messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f))
                    Text("Say hello to ${dmState.peerName}!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(dmState.messages) { msg ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.isMine) Arrangement.End else Arrangement.Start) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (msg.isMine) 16.dp else 4.dp, bottomEnd = if (msg.isMine) 4.dp else 16.dp))
                                .background(if (msg.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(msg.text, fontSize = 14.sp, color = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

private fun formatPostDate(ts: String): String = runCatching {
    ZonedDateTime.parse(ts).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}.getOrDefault(ts.take(10))
