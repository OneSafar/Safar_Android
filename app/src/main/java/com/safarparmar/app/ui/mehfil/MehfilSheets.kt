package com.safarparmar.app.ui.mehfil

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.Comment
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.domain.model.Sandesh
import com.safarparmar.app.ui.studyplanner.plan.PlanEyebrow
import com.safarparmar.app.ui.studyplanner.plan.PlanHairline
import com.safarparmar.app.ui.theme.LoraFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentsBottomSheet(
    post: MehfilPost,
    comments: List<Comment>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isPosting: Boolean,
    errorMessage: String?,
    commentInput: String,
    onCommentChange: (String) -> Unit,
    onPost: () -> Unit,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilFlatColors.Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MehfilFlatColors.Hairline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PlanEyebrow("Mehfil")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Comments",
                        fontFamily = LoraFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 22.sp,
                        color = MehfilFlatColors.Text,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${comments.size}", fontSize = 13.sp, color = MehfilFlatColors.Muted)
                }
            }
            PlanHairline(modifier = Modifier.padding(horizontal = 16.dp))
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MehfilFlatColors.Primary, strokeWidth = 2.dp)
                }
                comments.isEmpty() -> Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No comments yet. Be the first!", color = MehfilFlatColors.Muted, fontSize = 13.sp)
                }
                else -> CommentList(
                    comments = comments,
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.weight(1f),
                )
            }
            PlanHairline(modifier = Modifier.padding(horizontal = 16.dp))
            if (!errorMessage.isNullOrBlank()) {
                Text(
                    errorMessage,
                    color = MehfilFlatColors.Like,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
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
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MehfilFlatColors.Primary)
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MehfilFlatColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                comment.authorName.firstOrNull()?.uppercase() ?: "A",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MehfilFlatColors.Primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(comment.authorName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MehfilFlatColors.Text)
            Text(comment.content, fontSize = 13.sp, lineHeight = 18.sp, color = MehfilFlatColors.Text)
        }
    }
}

@Composable
private fun mehfilFlatFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MehfilFlatColors.Primary,
    unfocusedBorderColor = MehfilFlatColors.Hairline,
    focusedTextColor = MehfilFlatColors.Text,
    unfocusedTextColor = MehfilFlatColors.Text,
    cursorColor = MehfilFlatColors.Primary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedPlaceholderColor = MehfilFlatColors.Muted,
    unfocusedPlaceholderColor = MehfilFlatColors.Muted,
)

@Composable
private fun CommentInputRow(
    value: String,
    isPosting: Boolean,
    onValueChange: (String) -> Unit,
    onPost: () -> Unit,
) {
    val canSend = value.isNotBlank() && !isPosting
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Add a comment...", fontSize = 13.sp, color = MehfilFlatColors.Muted) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = mehfilFlatFieldColors(),
        )
        IconButton(
            onClick = { if (value.isNotBlank()) onPost() },
            enabled = canSend,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (canSend) MehfilFlatColors.Primary else MehfilFlatColors.Hairline.copy(alpha = 0.55f)),
        ) {
            if (isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (canSend) Color.White else MehfilFlatColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
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
    onPostComment: (String, String, () -> Unit) -> Unit,
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
        containerColor = MehfilFlatColors.Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MehfilFlatColors.Hairline) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp),
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
                            onPostComment(id, commentText) {
                                commentText = ""
                            }
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
    val canSend = commentText.isNotBlank()
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f)) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            PlanEyebrow("Sandesh")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, MehfilFlatColors.Hairline, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MehfilFlatColors.Text,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Text(
                    "Comments",
                    fontFamily = LoraFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 22.sp,
                    color = MehfilFlatColors.Text,
                    modifier = Modifier.weight(1f),
                )
                if (!isLoading) Text("${comments.size}", fontSize = 13.sp, color = MehfilFlatColors.Muted)
            }
        }
        PlanHairline(modifier = Modifier.padding(horizontal = 16.dp))
        if (isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), color = MehfilFlatColors.Primary, strokeWidth = 2.dp)
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
        PlanHairline(modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = commentText,
                onValueChange = onCommentTextChange,
                placeholder = { Text("Add a comment...", fontSize = 13.sp, color = MehfilFlatColors.Muted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = mehfilFlatFieldColors(),
            )
            IconButton(
                onClick = onPostComment,
                enabled = canSend,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (canSend) MehfilFlatColors.Primary else MehfilFlatColors.Hairline.copy(alpha = 0.55f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = if (canSend) Color.White else MehfilFlatColors.Muted,
                    modifier = Modifier.size(18.dp),
                )
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
        Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 40.dp),
    ) {
        PlanEyebrow("Mehfil")
        Text(
            "Sandesh",
            fontFamily = LoraFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 22.sp,
            color = MehfilFlatColors.Text,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Messages from the community",
            fontSize = 12.sp,
            color = MehfilFlatColors.Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        PlanHairline()
        sandeshes.forEachIndexed { index, sandesh ->
            val isReacted = sandesh.id in reactedSandeshIds
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(sandesh.content, fontSize = 14.sp, lineHeight = 20.sp, color = MehfilFlatColors.Text)
                Text(sandesh.createdAt.take(10), fontSize = 11.sp, color = MehfilFlatColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Row(
                        Modifier.clickable { onReact(sandesh.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            if (isReacted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = if (isReacted) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                        )
                        Text(
                            "${sandesh.reactionCount}",
                            fontSize = 12.sp,
                            color = if (isReacted) MehfilFlatColors.Like else MehfilFlatColors.Muted,
                        )
                    }
                    Row(
                        Modifier.clickable { onOpenComments(sandesh.id) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MehfilFlatColors.Primary,
                        )
                        Text(
                            "${sandesh.commentCount} comments",
                            fontSize = 12.sp,
                            color = MehfilFlatColors.Primary,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            if (index < sandeshes.lastIndex) {
                PlanHairline(alpha = 0.6f)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreatePostSheet(
    selectedSpace: String,
    isPosting: Boolean,
    onPost: (String, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var content by remember { mutableStateOf("") }
    var space by remember { mutableStateOf(if (selectedSpace == "ALL") "REFLECTIVE" else selectedSpace) }
    var isAnonymous by remember { mutableStateOf(false) }
    val canPost = content.isNotBlank() && !isPosting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilFlatColors.Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MehfilFlatColors.Hairline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlanEyebrow("Mehfil")
            Text(
                "New Post",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = MehfilFlatColors.Text,
            )
            PlanHairline()
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What's on your mind?", color = MehfilFlatColors.Muted) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = mehfilFlatFieldColors(),
            )
            Text("Space", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MehfilFlatColors.Muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("REFLECTIVE", "ACADEMIC").forEach { item ->
                    val selected = space == item
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(
                                if (selected) {
                                    Modifier.background(MehfilFlatColors.Primary)
                                } else {
                                    Modifier.border(1.dp, MehfilFlatColors.Hairline, CircleShape)
                                },
                            )
                            .clickable { space = item }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            item.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Color.White else MehfilFlatColors.Muted,
                        )
                    }
                }
            }
            AnonymousPostToggle(isAnonymous = isAnonymous, onCheckedChange = { isAnonymous = it })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (canPost) MehfilFlatColors.Primary
                        else MehfilFlatColors.Hairline.copy(alpha = 0.55f),
                    )
                    .clickable(enabled = canPost) {
                        if (content.isNotBlank()) onPost(content, space, isAnonymous)
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text(
                        if (isAnonymous) "Post without my name" else "Share post",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (canPost) Color.White else MehfilFlatColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnonymousPostToggle(isAnonymous: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isAnonymous) MehfilFlatColors.Primary.copy(alpha = 0.45f) else MehfilFlatColors.Hairline,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_ghost),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isAnonymous) MehfilFlatColors.Primary else MehfilFlatColors.Muted,
        )
        Column(Modifier.weight(1f)) {
            Text("Hide my name", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MehfilFlatColors.Text)
            Text("Your name won't be shown", fontSize = 11.sp, color = MehfilFlatColors.Muted)
        }
        Switch(
            checked = isAnonymous,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MehfilFlatColors.Primary,
                checkedBorderColor = MehfilFlatColors.Primary,
                uncheckedThumbColor = MehfilFlatColors.Muted,
                uncheckedTrackColor = MehfilFlatColors.Hairline.copy(alpha = 0.45f),
                uncheckedBorderColor = MehfilFlatColors.Hairline,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuidelinesSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MehfilFlatColors.Bg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = MehfilFlatColors.Hairline) },
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PlanEyebrow("Mehfil")
            Text(
                "Simple rules",
                fontFamily = LoraFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                color = MehfilFlatColors.Text,
            )
            PlanHairline()
            GuidelineItem(R.drawable.ic_graduation_cap, "Study room", "Ask about study, exams and careers.")
            GuidelineItem(R.drawable.ic_chat, "Talk room", "Share your thoughts and support other students.")
            GuidelineItem(R.drawable.ic_shield_check, "Be kind", "Do not post abuse, spam or unsafe content.")
            GuidelineItem(R.drawable.ic_ghost, "Keep it safe", "Unsafe posts may be removed and the account may be blocked.")
        }
    }
}

@Composable
private fun GuidelineItem(iconRes: Int, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MehfilFlatColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MehfilFlatColors.Primary,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MehfilFlatColors.Text)
            Text(desc, fontSize = 12.sp, color = MehfilFlatColors.Muted, lineHeight = 17.sp)
        }
    }
}
