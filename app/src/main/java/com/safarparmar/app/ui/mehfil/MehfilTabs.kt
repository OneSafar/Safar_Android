package com.safarparmar.app.ui.mehfil

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.ActivityItem
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.domain.model.Sandesh
import com.safarparmar.app.data.remote.dto.StudyCircleSummaryDto
import com.safarparmar.app.ui.components.PostCardSkeleton
import com.safarparmar.app.ui.components.SafarEmptyState
import com.safarparmar.app.ui.components.SafarPullRefreshBox
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily
import com.safarparmar.app.ui.theme.isLightBackground
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Small flat filled button — Primary fill, white text. Used for Accept actions. */
@Composable
private fun FlatPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MehfilFlatColors.Primary)
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

/** Small flat outline button — Hairline border, muted text. Used for Decline actions. */
@Composable
private fun FlatOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MehfilFlatColors.Muted)
    }
}

@Composable
internal fun CommunityTab(
    uiState: MehfilUiState,
    searchQuery: String,
    onClearSearch: () -> Unit,
    onSandeshCommentClick: (String) -> Unit,
    onCommentClick: (MehfilPost) -> Unit,
    onConnect: (MehfilPost) -> Unit,
    onLoadPosts: (Boolean) -> Unit,
    onJoinRoom: (String) -> Unit,
    onReactSandesh: (String) -> Unit,
    onLikePost: (MehfilPost) -> Unit,
    onSavePost: (String) -> Unit,
    onCreatePostClick: () -> Unit,
    onViewStudyCircles: () -> Unit,
    onOpenStudyCircle: (String) -> Unit,
) {
    val listState = rememberLazyListState()
    val filteredPosts = remember(uiState.posts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.posts
        } else {
            uiState.posts.filter { post ->
                post.content.contains(searchQuery, ignoreCase = true) ||
                    post.authorName.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    val showSandesh by remember {
        derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 80 }
    }
    val hasMore by rememberUpdatedState(uiState.hasMore)
    val isLoadingPosts by rememberUpdatedState(uiState.isLoadingPosts)

    LaunchedEffect(uiState.selectedSpace) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4
        }.collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingPosts) onLoadPosts(false)
        }
    }

    LaunchedEffect(uiState.posts.size) {
        if (hasMore && !isLoadingPosts) {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearBottom = total > 0 && lastVisible >= (total * 0.75f).toInt()
            val searchNeedsMore = searchQuery.isNotBlank() && filteredPosts.size < 8
            if (nearBottom || searchNeedsMore) onLoadPosts(false)
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (searchQuery.isNotBlank()) {
            CommunityHeader(
                resultCount = filteredPosts.size,
                searchQuery = searchQuery,
                onlineCount = uiState.onlineCount,
                socketConnected = uiState.socketConnected,
            )
        }

        Spacer(Modifier.height(4.dp))
        RoomSelector(selectedSpace = uiState.selectedSpace, onJoinRoom = onJoinRoom)
        Spacer(Modifier.height(6.dp))

        if (uiState.sandeshes.isNotEmpty()) {
            CollapsibleSandeshCard(
                sandeshes = uiState.sandeshes,
                reactedSandeshIds = uiState.reactedSandeshIds,
                onReact = onReactSandesh,
                onCommentClick = onSandeshCommentClick,
            )
            Spacer(Modifier.height(6.dp))
        }

        when {
            uiState.isLoadingPosts && uiState.posts.isEmpty() -> LoadingPostList()
            !uiState.isLoadingPosts && uiState.posts.isEmpty() -> SafarEmptyState(
                title = "No posts yet",
                message = "Be the first to share in this room.",
                modifier = Modifier.fillMaxSize(),
            )
            searchQuery.isNotBlank() && filteredPosts.isEmpty() -> EmptySearchState(
                searchQuery = searchQuery,
                isSearchingMore = uiState.isLoadingPosts && uiState.hasMore,
                onClearSearch = onClearSearch,
            )
            else -> SafarPullRefreshBox(
                isRefreshing = uiState.isLoadingPosts && uiState.posts.isNotEmpty(),
                onRefresh = { onLoadPosts(true) },
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "__community_posts_header__") {
                        Text(
                            "COMMUNITY POSTS",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MehfilFlatColors.Muted,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }

                    items(
                        items = filteredPosts,
                        key = { it.id },
                        contentType = { "post" },
                    ) { post ->
                        val isSaved = remember(post.id, uiState.savedPostIds) { post.id in uiState.savedPostIds }
                        val onLike = remember(post, onLikePost) { { onLikePost(post) } }
                        val onComment = remember(post, onCommentClick) { { onCommentClick(post) } }
                        val onSave = remember(post.id, onSavePost) { { onSavePost(post.id) } }
                        val onConnectPost = remember(post, onConnect) { { onConnect(post) } }
                        PostCard(
                            post = post,
                            isSaved = isSaved,
                            currentUserId = uiState.currentUserId,
                            mehfilDm = uiState.mehfilDm,
                            isLoadingPremiumFeatures = uiState.isLoadingPremiumFeatures,
                            onLike = onLike,
                            onComment = onComment,
                            onSave = onSave,
                            onConnect = onConnectPost,
                        )
                    }
                    if (uiState.isLoadingPosts && uiState.posts.isNotEmpty()) {
                        item(key = "__loading__") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MehfilFlatColors.Primary, strokeWidth = 2.dp)
                                    if (searchQuery.isNotBlank()) {
                                        Text("Searching more pages...", fontSize = 12.sp, color = MehfilFlatColors.Muted)
                                    }
                                }
                            }
                        }
                    }
                    if (searchQuery.isNotBlank() && !uiState.hasMore && filteredPosts.isNotEmpty()) {
                        item(key = "__end__") {
                            Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                Text("All matching posts shown", fontSize = 12.sp, color = MehfilFlatColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityHeader(
    resultCount: Int,
    searchQuery: String,
    onlineCount: Int,
    socketConnected: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val resultText = if (searchQuery.isBlank()) {
            "Community Space"
        } else {
            "$resultCount result${if (resultCount != 1) "s" else ""} for \"$searchQuery\""
        }
        Text(
            resultText,
            fontFamily = LoraFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
            color = if (searchQuery.isBlank()) MehfilFlatColors.Text else MehfilFlatColors.Primary,
        )
        when {
            onlineCount > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MehfilFlatColors.Activity))
                Text("$onlineCount online", fontSize = 11.sp, color = MehfilFlatColors.Activity)
            }
            !socketConnected -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MehfilFlatColors.Muted)
                Text("Connecting...", fontSize = 11.sp, color = MehfilFlatColors.Muted)
            }
        }
    }
}

@Composable
private fun RoomSelector(selectedSpace: String, onJoinRoom: (String) -> Unit) {
    val rooms = listOf(
        "ALL" to "All",
        "ACADEMIC" to "Academic",
        "REFLECTIVE" to "Reflective",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rooms.forEach { (room, label) ->
            val selected = selectedSpace.equals(room, ignoreCase = true)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (selected) {
                            Modifier.background(MehfilFlatColors.Primary)
                        } else {
                            Modifier
                                .background(MehfilFlatColors.Surface)
                                .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(10.dp))
                        },
                    )
                    .clickable { onJoinRoom(room) }
                    .padding(horizontal = if (room == "ALL") 24.dp else 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else MehfilFlatColors.Text,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StudyCircleShelf(
    circles: List<StudyCircleSummaryDto>,
    isLoading: Boolean,
    onViewAll: () -> Unit,
    onOpenCircle: (String) -> Unit,
) {
    val visibleCircles = circles.take(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Study Circles", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B168D))
                Text("Focus together, grow together", fontSize = 12.sp, color = MehfilFlatColors.Muted)
            }
            FlatOutlineButton(text = "View all", onClick = onViewAll)
        }

        when {
            isLoading -> Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF6B168D), strokeWidth = 2.dp)
            }
            visibleCircles.isEmpty() -> Row(
                Modifier.fillMaxWidth().clickable(onClick = onViewAll).padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFF6B168D))
                Column(Modifier.weight(1f)) {
                    Text("Find your study people", fontWeight = FontWeight.SemiBold, color = MehfilFlatColors.Text)
                    Text("Join or create your first circle", fontSize = 12.sp, color = MehfilFlatColors.Muted)
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                visibleCircles.forEach { circle ->
                    StudyCircleShelfCard(circle, { onOpenCircle(circle.id) }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StudyCircleShelfCard(circle: StudyCircleSummaryDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val purple = Color(0xFF6B168D)
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, purple.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).border(1.5.dp, purple, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                if (circle.isPinned) Icons.Default.VerifiedUser else if (circle.visibility.equals("public", true)) Icons.Default.Language else Icons.Default.Lock,
                contentDescription = if (circle.isPinned) "Official" else circle.visibility,
                tint = purple,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(circle.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.Text)
        Text("${circle.focusingCount} focusing", fontSize = 11.sp, color = purple, fontWeight = FontWeight.SemiBold)
        Box(Modifier.clip(CircleShape).background(purple.copy(alpha = 0.10f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text("Open", fontSize = 12.sp, color = purple, fontWeight = FontWeight.Bold)
        }
    }
}


@Composable
private fun LoadingPostList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(5) { PostCardSkeleton() }
    }
}

@Composable
private fun EmptySearchState(searchQuery: String, isSearchingMore: Boolean, onClearSearch: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSearchingMore) {
                CircularProgressIndicator(color = MehfilFlatColors.Primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Text("Searching posts...", color = MehfilFlatColors.Muted, fontSize = 13.sp)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_magnifying_glass),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MehfilFlatColors.Muted,
                )
                Text("No results for \"$searchQuery\"", color = MehfilFlatColors.Muted, fontSize = 14.sp)
                Text(
                    "Clear search",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MehfilFlatColors.Primary,
                    modifier = Modifier
                        .clickable(onClick = onClearSearch)
                        .padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun CollapsibleSandeshCard(
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String>,
    onReact: (String) -> Unit,
    onCommentClick: (String) -> Unit,
) {
    if (sandeshes.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_megaphone),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MehfilFlatColors.Primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "SANDESH",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MehfilFlatColors.Muted,
                )
                Text(
                    if (sandeshes.size == 1) "1 announcement" else "${sandeshes.size} announcements",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MehfilFlatColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MehfilFlatColors.Muted,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                sandeshes.forEach { sandesh ->
                    SandeshAnnouncementCard(
                        sandesh = sandesh,
                        isReacted = sandesh.id in reactedSandeshIds,
                        onReact = onReact,
                        onCommentClick = onCommentClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun SandeshAnnouncementCard(
    sandesh: Sandesh,
    isReacted: Boolean,
    onReact: (String) -> Unit,
    onCommentClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(MehfilFlatColors.Connect),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = "Admin", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Parmar Sir's Corner", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MehfilFlatColors.Text)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MehfilFlatColors.Connect.copy(alpha = 0.14f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text("Faculty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.Connect)
                    }
                }
                Text(formatPostDate(sandesh.createdAt), fontSize = 11.sp, color = MehfilFlatColors.Muted)
            }
        }
        Text(sandesh.content, fontSize = 14.sp, lineHeight = 20.sp, color = MehfilFlatColors.Text)
        SandeshMedia(sandesh = sandesh)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clickable { onReact(sandesh.id) }.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(
                    if (isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (isReacted) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                )
                Text("${sandesh.reactionCount}", fontSize = 11.sp, color = if (isReacted) MehfilFlatColors.Like else MehfilFlatColors.Muted)
            }
            Row(Modifier.clickable { onCommentClick(sandesh.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MehfilFlatColors.Primary)
                Text("${sandesh.commentCount}", fontSize = 11.sp, color = MehfilFlatColors.Primary)
            }
        }
    }
}

@Composable
private fun SandeshMedia(sandesh: Sandesh) {
    val context = LocalContext.current
    val youtubeRegex = Regex("""(?:https?://)?(?:www\.)?(?:youtube\.com/(?:[^/\n\s]+/\S+/|(?:v|e(?:mbed)?)/|\S*?[?&]v=)|youtu\.be/)([a-zA-Z0-9_-]{11})""")
    val imageVideoId = if (sandesh.imageUrl.contains("img.youtube.com")) {
        Regex("""img\.youtube\.com/vi/([a-zA-Z0-9_-]{11})""").find(sandesh.imageUrl)?.groupValues?.get(1)
    } else {
        null
    }
    val contentVideoId = youtubeRegex.find(sandesh.content)?.groupValues?.get(1)
    val youtubeVideoId = imageVideoId ?: contentVideoId
    val directImageUrl = if (youtubeVideoId == null && sandesh.imageUrl.isNotBlank()) sandesh.imageUrl else null

    if (youtubeVideoId != null) {
        val thumbUrl = "https://img.youtube.com/vi/$youtubeVideoId/hqdefault.jpg"
        val videoUrl = "https://www.youtube.com/watch?v=$youtubeVideoId"
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                // No app on the device handles the link (no browser / stripped ROM) →
                // ActivityNotFoundException. Tell the user instead of crashing.
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
                } catch (e: android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(context, "No app available to open this video.", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = "YouTube thumbnail",
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                contentScale = ContentScale.Crop,
            )
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.25f)))
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.75f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    } else if (directImageUrl != null) {
        AsyncImage(
            model = directImageUrl,
            contentDescription = "Parmar Sir's Corner attachment. Tap to open.",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .heightIn(max = 220.dp)
                .clickable {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(directImageUrl)))
                    } catch (e: android.content.ActivityNotFoundException) {
                        android.widget.Toast.makeText(context, "No app available to open this image.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun PostCard(
    post: MehfilPost,
    isSaved: Boolean,
    currentUserId: String,
    mehfilDm: Boolean,
    isLoadingPremiumFeatures: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onConnect: () -> Unit,
) {
    val canConnect = post.userId.isNotBlank() && post.userId != currentUserId
    val isConnectLocked = canConnect && !mehfilDm && !isLoadingPremiumFeatures
    val tagColor = spaceColor(post.space)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDE9FE)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    post.authorName.firstOrNull()?.uppercase() ?: "A",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MehfilFlatColors.Primary,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    post.authorName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.5.sp,
                    color = MehfilFlatColors.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatPostDate(post.createdAt),
                    fontSize = 11.5.sp,
                    color = MehfilFlatColors.Muted,
                )
            }
            if (post.space.isNotBlank()) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, tagColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        post.space.uppercase(),
                        fontSize = 10.sp,
                        color = tagColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }

        Text(
            post.content,
            fontSize = 14.5.sp,
            lineHeight = 21.sp,
            color = MehfilFlatColors.Text,
            modifier = Modifier.padding(top = 2.dp),
        )

        HorizontalDivider(
            thickness = 0.8.dp,
            color = MehfilFlatColors.Hairline.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 2.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onLike),
            ) {
                Icon(
                    if (post.userLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    modifier = Modifier.size(18.dp),
                    tint = if (post.userLiked) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                )
                Text(
                    "${post.reactionCount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (post.userLiked) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable(onClick = onComment),
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment",
                    modifier = Modifier.size(18.dp),
                    tint = MehfilFlatColors.Muted,
                )
                Text(
                    "${post.commentCount}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MehfilFlatColors.Muted,
                )
            }

            if (canConnect) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.clickable(onClick = onConnect),
                ) {
                    Icon(
                        if (isConnectLocked) Icons.Default.Lock else Icons.Default.PersonAdd,
                        contentDescription = "Connect",
                        modifier = Modifier.size(17.dp),
                        tint = if (isConnectLocked) MehfilFlatColors.Muted else MehfilFlatColors.Connect,
                    )
                    Text(
                        "Connect",
                        fontSize = 12.5.sp,
                        color = if (isConnectLocked) MehfilFlatColors.Muted else MehfilFlatColors.Connect,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            Icon(
                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                modifier = Modifier
                    .size(18.dp)
                    .clickable(onClick = onSave),
                tint = if (isSaved) MehfilFlatColors.Primary else MehfilFlatColors.Muted,
            )
        }
    }
}

@Composable
internal fun SavedTab(
    uiState: MehfilUiState,
    onLikePost: (MehfilPost) -> Unit,
    onCommentClick: (MehfilPost) -> Unit,
    onUnsavePost: (String) -> Unit,
    onConnect: (MehfilPost) -> Unit,
) {
    when {
        uiState.isLoadingSaved -> LoadingPostList()
        uiState.savedPosts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_bookmarks_simple), contentDescription = null, modifier = Modifier.size(48.dp), tint = MehfilFlatColors.Muted)
                Text("No saved posts yet", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MehfilFlatColors.Muted)
            }
        }
        else -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Saved Posts",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MehfilFlatColors.Text,
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.savedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isSaved = true,
                        currentUserId = uiState.currentUserId,
                        mehfilDm = uiState.mehfilDm,
                        isLoadingPremiumFeatures = uiState.isLoadingPremiumFeatures,
                        onLike = { onLikePost(post) },
                        onComment = { onCommentClick(post) },
                        onSave = { onUnsavePost(post.id) },
                        onConnect = { onConnect(post) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsTab(uiState: MehfilUiState) {
    if (uiState.isLoadingActivity) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MehfilFlatColors.Primary)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text(
            "My Activity",
            fontFamily = LoraFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MehfilFlatColors.Text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        val activityCounts = remember(uiState.activity) {
            Triple(
                uiState.activity.count { it.type == "post" },
                uiState.activity.count { it.type == "comment" },
                uiState.activity.count { it.type == "like" },
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityStatCard("Posts", "${activityCounts.first}", Icons.AutoMirrored.Filled.Article, Modifier.weight(1f))
            ActivityStatCard("Comments", "${activityCounts.second}", Icons.Default.ChatBubble, Modifier.weight(1f))
            ActivityStatCard("Likes", "${activityCounts.third}", Icons.Default.Favorite, Modifier.weight(1f))
        }
        Spacer(Modifier.size(8.dp))
        if (uiState.activity.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No activity yet.", color = MehfilFlatColors.Muted)
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = uiState.activity,
                key = { index, item -> "${item.type}:${item.thoughtId}:${item.createdAt}:${item.comment.orEmpty()}:$index" },
                contentType = { _, _ -> "activity" },
            ) { _, item ->
                ActivityRow(item = item)
            }
        }
    }
}

@Composable
private fun ActivityStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MehfilFlatColors.Activity, modifier = Modifier.size(18.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MehfilFlatColors.Text)
        Text(label, fontSize = 10.sp, color = MehfilFlatColors.Muted)
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(
                id = when (item.type) {
                    "post" -> R.drawable.ic_pencil_simple_line
                    "comment" -> R.drawable.ic_chat
                    "like" -> R.drawable.ic_heart_straight
                    else -> R.drawable.ic_push_pin
                },
            ),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MehfilFlatColors.Activity,
        )
        Column(Modifier.weight(1f)) {
            Text(
                when (item.type) {
                    "post" -> "Posted"
                    "comment" -> "Commented: ${item.comment ?: ""}"
                    "like" -> "Liked a post"
                    else -> item.type
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MehfilFlatColors.Text,
            )
            Text(item.thoughtContent.take(60), fontSize = 12.sp, color = MehfilFlatColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(item.createdAt.take(10), fontSize = 10.sp, color = MehfilFlatColors.Muted)
    }
}

@Composable
internal fun ConnectionsTab(
    uiState: MehfilUiState,
    onNavigateToDmChat: () -> Unit,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
) {
    val pending = uiState.pendingDmRequests
    val dmState = uiState.dmState

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (pending.isNotEmpty()) {
            PendingRequestsCard(pending = pending, onAcceptDm = onAcceptDm, onDeclineDm = onDeclineDm)
        }
        when (dmState) {
            is DmState.Idle -> ConnectIdleCard()
            is DmState.Waiting -> Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MehfilFlatColors.Chats)
                Text("Waiting for ${dmState.userName} to reply", fontSize = 13.sp, color = MehfilFlatColors.Muted)
            }
            is DmState.IncomingRequest -> IncomingRequestCard(dmState = dmState, onAcceptDm = onAcceptDm, onDeclineDm = onDeclineDm)
            is DmState.Open -> Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MehfilFlatColors.Surface)
                    .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
                    .clickable { onNavigateToDmChat() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniDmAvatar(name = dmState.peerName, avatarUrl = dmState.peerAvatar)
                Text("Chat with ${dmState.peerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MehfilFlatColors.Text, modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MehfilFlatColors.Chats)
            }
        }
        uiState.dmError?.let { Text(it, color = MehfilFlatColors.Like, fontSize = 12.sp) }
    }
}

@Composable
private fun PendingRequestsCard(
    pending: List<PendingDmRequest>,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MehfilFlatColors.Chats, modifier = Modifier.size(14.dp))
            Text("Chat requests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MehfilFlatColors.Chats)
        }
        pending.forEach { request ->
            var acceptingThis by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniDmAvatar(name = request.userName, avatarUrl = request.userAvatar)
                Text(request.userName, fontSize = 13.sp, color = MehfilFlatColors.Text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (acceptingThis) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp).padding(2.dp), strokeWidth = 2.dp, color = MehfilFlatColors.Primary)
                } else {
                    FlatPrimaryButton(
                        text = "Accept",
                        onClick = {
                            acceptingThis = true
                            onAcceptDm(request.userId)
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
                FlatOutlineButton(
                    text = "Decline",
                    onClick = { onDeclineDm(request.userId) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun ConnectIdleCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Private chat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MehfilFlatColors.Text)
        Text("Tap Connect on a post to ask that student to chat.", fontSize = 12.sp, color = MehfilFlatColors.Muted, lineHeight = 16.sp)
    }
}

@Composable
private fun IncomingRequestCard(
    dmState: DmState.IncomingRequest,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MehfilFlatColors.Surface)
            .border(1.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniDmAvatar(name = dmState.fromUserName, avatarUrl = dmState.fromUserAvatar)
            Spacer(Modifier.width(6.dp))
            Text("${dmState.fromUserName} wants to connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MehfilFlatColors.Text)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlatPrimaryButton(
                text = "Accept",
                onClick = { onAcceptDm(dmState.fromUserId) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            )
            FlatOutlineButton(
                text = "Decline",
                onClick = { onDeclineDm(dmState.fromUserId) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MiniDmAvatar(name: String, avatarUrl: String?) {
    Box(
        Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MehfilFlatColors.Primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$name profile photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                name.firstOrNull()?.uppercase() ?: "?",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MehfilFlatColors.Primary,
            )
        }
    }
}

@Composable
private fun spaceColor(space: String): Color = when (space.uppercase()) {
    "ACADEMIC" -> MehfilFlatColors.Saved
    "REFLECTIVE" -> MehfilFlatColors.Activity
    else -> MehfilFlatColors.Primary
}

internal fun formatPostDate(ts: String): String = runCatching {
    val zdt = ZonedDateTime.parse(ts).withZoneSameInstant(ZoneId.of("Asia/Kolkata"))
    val now = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
    val pattern = if (zdt.year == now.year) "d MMM, h:mm a" else "d MMM yyyy, h:mm a"
    val formatted = zdt.format(DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH))
    formatted.replace(" AM", " am").replace(" PM", " pm")
}.getOrDefault(ts.take(16).replace('T', ' '))
