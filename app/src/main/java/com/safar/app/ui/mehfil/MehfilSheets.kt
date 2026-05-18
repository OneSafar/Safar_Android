package com.safar.app.ui.mehfil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safar.app.R
import com.safar.app.domain.model.Comment
import com.safar.app.domain.model.MehfilPost
import com.safar.app.domain.model.Sandesh
import com.safar.app.ui.theme.Red500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsBottomSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Comments",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("${comments.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary)
                }
                comments.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No comments yet. Be the first!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                else -> CommentList(
                    comments = comments,
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.weight(1f),
                )
            }
            HorizontalDivider()
            CommentInputRow(
                value = commentInput,
                isPosting = isPosting,
                onValueChange = onCommentChange,
                onPost = onPost,
            )
        }
    }
}

@Composable
private fun CommentList(
    comments: List<Comment>,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commentsListState = rememberLazyListState()
    LaunchedEffect(commentsListState) {
        snapshotFlow {
            val info = commentsListState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 2
        }.collect { nearEnd ->
            if (nearEnd && hasMore && !isLoadingMore) onLoadMore()
        }
    }
    LazyColumn(
        state = commentsListState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(comments, key = { it.id.ifBlank { "${it.authorName}:${it.createdAt}:${it.content}" } }) { comment ->
            CommentRow(comment = comment)
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

@Composable
private fun CommentRow(comment: Comment) {
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

@Composable
private fun CommentInputRow(
    value: String,
    isPosting: Boolean,
    onValueChange: (String) -> Unit,
    onPost: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Add a comment...", fontSize = 13.sp) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
        )
        IconButton(
            onClick = { if (value.isNotBlank()) onPost() },
            modifier = Modifier.size(44.dp).clip(CircleShape).background(
                if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f),
            ),
        ) {
            if (isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.Send, contentDescription = null, tint = if (value.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SandeshBottomSheet(
    sandesh: Sandesh,
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String>,
    sandeshComments: List<Comment>,
    isLoadingSandeshComments: Boolean,
    isLoadingMoreSandeshComments: Boolean,
    hasMoreSandeshComments: Boolean,
    initialCommentTargetId: String?,
    onReact: (String) -> Unit,
    onLoadComments: (String) -> Unit,
    onLoadMoreComments: (String) -> Unit,
    onPostComment: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var commentTargetId by remember { mutableStateOf<String?>(initialCommentTargetId) }
    var commentText by remember { mutableStateOf("") }
    val showComments = commentTargetId != null

    LaunchedEffect(commentTargetId) {
        commentTargetId?.let { onLoadComments(it) }
    }

    ModalBottomSheet(
        onDismissRequest = { if (showComments && initialCommentTargetId == null) commentTargetId = null else onDismiss() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        AnimatedContent(
            targetState = showComments,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "sandesh_nav",
        ) { inComments ->
            if (inComments) {
                SandeshCommentsPane(
                    comments = sandeshComments,
                    isLoading = isLoadingSandeshComments,
                    isLoadingMore = isLoadingMoreSandeshComments,
                    hasMore = hasMoreSandeshComments,
                    commentText = commentText,
                    onBack = {
                        commentTargetId = null
                        commentText = ""
                    },
                    onCommentTextChange = { commentText = it },
                    onLoadMore = { commentTargetId?.let(onLoadMoreComments) },
                    onPostComment = {
                        val id = commentTargetId
                        if (commentText.isNotBlank() && id != null) {
                            onPostComment(id, commentText)
                            commentText = ""
                        }
                    },
                )
            } else {
                SandeshListPane(
                    sandeshes = sandeshes,
                    reactedSandeshIds = reactedSandeshIds,
                    onReact = onReact,
                    onOpenComments = { commentTargetId = it },
                )
            }
        }
    }
}

@Composable
private fun SandeshCommentsPane(
    comments: List<Comment>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    commentText: String,
    onBack: () -> Unit,
    onCommentTextChange: (String) -> Unit,
    onLoadMore: () -> Unit,
    onPostComment: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
            }
            Text("Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            if (!isLoading) Text("${comments.size}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider()
        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.primary)
            }
        } else {
            CommentList(
                comments = comments,
                isLoadingMore = isLoadingMore,
                hasMore = hasMore,
                onLoadMore = onLoadMore,
                modifier = Modifier.weight(1f),
            )
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            IconButton(
                onClick = onPostComment,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(
                    if (commentText.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.2f),
                ),
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = if (commentText.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SandeshListPane(
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String>,
    onReact: (String) -> Unit,
    onOpenComments: (String) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().fillMaxHeight(0.92f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Sandesh", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Messages from the community", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        sandeshes.forEach { sandesh ->
            val isReacted = sandesh.id in reactedSandeshIds
            Card(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(0.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(sandesh.content, fontSize = 14.sp, lineHeight = 20.sp)
                    Text(sandesh.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Row(Modifier.clickable { onReact(sandesh.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(
                                if (isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text("${sandesh.reactionCount}", fontSize = 12.sp, color = if (isReacted) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(Modifier.clickable { onOpenComments(sandesh.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Text("${sandesh.commentCount} comments", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostSheet(selectedSpace: String, onPost: (String, String, Boolean) -> Unit, onDismiss: () -> Unit) {
    var content by remember { mutableStateOf("") }
    var space by remember { mutableStateOf(if (selectedSpace == "ALL") "REFLECTIVE" else selectedSpace) }
    var isAnonymous by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("New Post", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
            )
            Text("Space", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("REFLECTIVE", "ACADEMIC").forEach { item ->
                    FilterChip(
                        selected = space == item,
                        onClick = { space = item },
                        label = { Text(item.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                    )
                }
            }
            AnonymousPostToggle(isAnonymous = isAnonymous, onCheckedChange = { isAnonymous = it })
            Button(
                onClick = { if (content.isNotBlank()) onPost(content, space, isAnonymous) },
                enabled = content.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(if (isAnonymous) "Post Anonymously" else "Post", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AnonymousPostToggle(isAnonymous: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isAnonymous) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, if (isAnonymous) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_ghost), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text("Post Anonymously", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Your name won't be shown", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isAnonymous,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedBorderColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuidelinesSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Community Guidelines", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Posting Rules", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            GuidelineItem(R.drawable.ic_graduation_cap, "Academic Hall", "Research, study hacks, and career help only. No venting.")
            GuidelineItem(R.drawable.ic_chat, "Thoughts", "Emotional support and venting. Move here for personal struggles.")
            Text("Consequences", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            GuidelineItem(R.drawable.ic_shield_check, "Report-Based Bans", "1+ reports trigger automatic bans (2D -> 7D -> Permanent).")
            GuidelineItem(R.drawable.ic_ghost, "Shadow Banning", "Repeated spam results in silent silencing; others won't see you.")
        }
    }
}

@Composable
private fun GuidelineItem(iconRes: Int, title: String, desc: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 17.sp)
        }
    }
}
