package com.safar.app.ui.mehfil

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.safar.app.R
import com.safar.app.domain.model.ActivityItem
import com.safar.app.domain.model.Comment
import com.safar.app.domain.model.MehfilPost
import com.safar.app.domain.model.Sandesh
import com.safar.app.ui.components.PostCardSkeleton
import com.safar.app.ui.components.SafarEmptyState
import com.safar.app.ui.components.SafarPullRefreshBox
import com.safar.app.ui.theme.Blue500
import com.safar.app.ui.theme.Green500
import com.safar.app.ui.theme.Red500
import com.safar.app.ui.theme.Rose900
import com.safar.app.ui.theme.Teal600
import com.safar.app.ui.theme.Violet500
import com.safar.app.ui.theme.Violet600
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        CommunityHeader(
            resultCount = filteredPosts.size,
            searchQuery = searchQuery,
            onlineCount = uiState.onlineCount,
            socketConnected = uiState.socketConnected,
        )

        AnimatedVisibility(
            visible = showSandesh && uiState.sandeshes.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            CollapsibleSandeshCard(
                sandeshes = uiState.sandeshes,
                reactedSandeshIds = uiState.reactedSandeshIds,
                onReact = onReactSandesh,
                onCommentClick = onSandeshCommentClick,
            )
        }

        Text(
            "The student lounge for unfiltered thoughts and academic life-hacks.",
            fontSize = 12.sp,
            lineHeight = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Spacer(Modifier.size(6.dp))
        RoomSelector(selectedSpace = uiState.selectedSpace, onJoinRoom = onJoinRoom)
        Spacer(Modifier.size(4.dp))

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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(
                        items = filteredPosts,
                        key = { it.id },
                        contentType = { "post" },
                    ) { post ->
                        val isSaved = remember(post.id, uiState.savedPostIds) { post.id in uiState.savedPostIds }
                        val onLike = remember(post, onLikePost) { { onLikePost(post) } }
                        val onComment = remember(post, onCommentClick) { { onCommentClick(post) } }
                        val onSave = remember(post.id, onSavePost) { { onSavePost(post.id) } }
                        val onConnectPost = remember(post, uiState.currentUserId, onConnect) {
                            if (post.userId.isNotEmpty() && post.userId != uiState.currentUserId) {
                                { onConnect(post) }
                            } else {
                                null
                            }
                        }
                        PostCard(
                            post = post,
                            isSaved = isSaved,
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
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                    if (searchQuery.isNotBlank()) {
                                        Text("Searching more pages...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
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
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
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
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
            color = if (searchQuery.isBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary,
        )
        when {
            onlineCount > 0 -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Green500))
                Text("$onlineCount online", fontSize = 11.sp, color = Green500)
            }
            !socketConnected -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Connecting...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RoomSelector(selectedSpace: String, onJoinRoom: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
    ) {
        val rooms = listOf(
            Triple("ALL", "All", Rose900),
            Triple("ACADEMIC", "Academic Hall", Teal600),
            Triple("REFLECTIVE", "Thoughts", Violet600),
        )
        rooms.forEach { (room, label, color) ->
            val selected = selectedSpace == room
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (selected) color else Color.Transparent)
                    .clickable { onJoinRoom(room) }
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
}

@Composable
private fun LoadingPostList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(5) { PostCardSkeleton() }
    }
}

@Composable
private fun EmptySearchState(searchQuery: String, isSearchingMore: Boolean, onClearSearch: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSearchingMore) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                Text("Searching posts...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.ic_magnifying_glass),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("No results for \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                TextButton(onClick = onClearSearch) { Text("Clear search") }
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

    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(id = R.drawable.ic_megaphone), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text("SANDESH", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.primary)
                    Text(if (sandeshes.size == 1) "1 new announcement" else "${sandeshes.size} announcements", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFF4F46E5)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.VerifiedUser, contentDescription = "Admin", tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Parmar Sir's Corner", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Box(Modifier.background(Color(0xFFE0E7FF), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text("Faculty", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                    }
                }
                Text(formatPostDate(sandesh.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(sandesh.content, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
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
                    tint = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("${sandesh.reactionCount}", fontSize = 11.sp, color = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.clickable { onCommentClick(sandesh.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Text("${sandesh.commentCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
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
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl)))
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
            contentDescription = "Attached image",
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).heightIn(max = 220.dp),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun PostCard(
    post: MehfilPost,
    isSaved: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onSave: () -> Unit,
    onConnect: (() -> Unit)?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Text(post.authorName.firstOrNull()?.uppercase() ?: "A", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f)) {
                    Text(post.authorName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatPostDate(post.createdAt), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (post.space.isNotEmpty()) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(spaceColor(post.space).copy(0.15f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(post.space, fontSize = 9.sp, color = spaceColor(post.space), fontWeight = FontWeight.Bold)
                    }
                }
                if (onConnect != null) {
                    Box(
                        modifier = Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)).clickable(onClick = onConnect),
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
                    Icon(if (post.userLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, modifier = Modifier.size(17.dp), tint = if (post.userLiked) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${post.reactionCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.clickable(onClick = onComment)) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${post.commentCount}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp).clickable(onClick = onSave),
                    tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SavedTab(
    uiState: MehfilUiState,
    onLikePost: (MehfilPost) -> Unit,
    onCommentClick: (MehfilPost) -> Unit,
    onUnsavePost: (String) -> Unit,
) {
    when {
        uiState.isLoadingSaved -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        uiState.savedPosts.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(painter = painterResource(id = R.drawable.ic_bookmarks_simple), contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No saved posts yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Saved Posts", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text("${uiState.savedPosts.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = uiState.savedPosts,
                    key = { it.id },
                    contentType = { "savedPost" },
                ) { post ->
                    val onLike = remember(post, onLikePost) { { onLikePost(post) } }
                    val onComment = remember(post, onCommentClick) { { onCommentClick(post) } }
                    val onUnsave = remember(post.id, onUnsavePost) { { onUnsavePost(post.id) } }
                    PostCard(post = post, isSaved = true, onLike = onLike, onComment = onComment, onSave = onUnsave, onConnect = null)
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsTab(uiState: MehfilUiState) {
    if (uiState.isLoadingActivity) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }
    Column(Modifier.fillMaxSize()) {
        Text("My Activity", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        val activityCounts = remember(uiState.activity) {
            Triple(
                uiState.activity.count { it.type == "post" },
                uiState.activity.count { it.type == "comment" },
                uiState.activity.count { it.type == "like" },
            )
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityStatCard("Posts", "${activityCounts.first}", Icons.Default.Article, Modifier.weight(1f))
            ActivityStatCard("Comments", "${activityCounts.second}", Icons.Default.ChatBubble, Modifier.weight(1f))
            ActivityStatCard("Likes", "${activityCounts.third}", Icons.Default.Favorite, Modifier.weight(1f))
        }
        Spacer(Modifier.size(8.dp))
        if (uiState.activity.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No activity yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = uiState.activity,
                key = { item -> "${item.type}:${item.thoughtId}:${item.createdAt}:${item.comment.orEmpty()}" },
                contentType = { "activity" },
            ) { item ->
                ActivityRow(item = item)
            }
        }
    }
}

@Composable
private fun ActivityStatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
                tint = MaterialTheme.colorScheme.onSurface,
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
                )
                Text(item.thoughtContent.take(60), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(item.createdAt.take(10), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                Text("Waiting for response...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is DmState.IncomingRequest -> IncomingRequestCard(dmState = dmState, onAcceptDm = onAcceptDm, onDeclineDm = onDeclineDm)
            is DmState.Open -> Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToDmChat() },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Green500))
                    Text("Chat with ${dmState.peerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        uiState.dmError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
    }
}

@Composable
private fun PendingRequestsCard(
    pending: List<PendingDmRequest>,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text("Incoming Requests", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }
            pending.forEach { request ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(0.12f)), contentAlignment = Alignment.Center) {
                        Text(request.userName.firstOrNull()?.uppercase() ?: "?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(request.userName, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextButton(onClick = { onAcceptDm(request.userId) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text("Accept", fontSize = 12.sp)
                    }
                    TextButton(onClick = { onDeclineDm(request.userId) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text("Decline", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectIdleCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Connect with someone", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Tap Connect on any post to start a private chat.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun IncomingRequestCard(
    dmState: DmState.IncomingRequest,
    onAcceptDm: (String) -> Unit,
    onDeclineDm: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painter = painterResource(id = R.drawable.ic_envelope_simple), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text("${dmState.fromUserName} wants to connect", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAcceptDm(dmState.fromUserId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Text("Accept", fontSize = 13.sp)
                }
                OutlinedButton(onClick = { onDeclineDm(dmState.fromUserId) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    Text("Decline", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun spaceColor(space: String): Color = when (space.uppercase()) {
    "ACADEMIC" -> Blue500
    "REFLECTIVE" -> Violet500
    else -> MaterialTheme.colorScheme.primary
}

internal fun formatPostDate(ts: String): String = runCatching {
    ZonedDateTime.parse(ts).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}.getOrDefault(ts.take(10))
