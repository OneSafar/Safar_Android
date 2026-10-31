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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import com.composables.ui.components.Icon
import com.composables.ui.components.IconButton
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.BottomSheetDetent
import com.composables.ui.components.rememberBottomSheetState
import com.safarparmar.app.ui.components.popups.SafarComposablesBottomSheet
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.ui.components.Switch
import com.composables.ui.components.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safarparmar.app.R
import com.safarparmar.app.ui.tutorial.tutorialTarget
import com.safarparmar.app.domain.model.Comment
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.domain.model.Sandesh
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
    MehfilSheet(
        onDismissRequest = onDismiss,
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
                MehfilSheetEyebrow(stringResource(R.string.mehfil_title))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.mehfil_comments),
                        fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = MehfilFlatColors.Text,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text("${comments.size}", fontSize = 13.sp, color = MehfilFlatColors.Muted)
                }
            }
            MehfilSheetDivider(modifier = Modifier.padding(horizontal = 16.dp))
            when {
                isLoading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MehfilFlatColors.Primary, strokeWidth = 2.dp)
                }
                comments.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.mehfil_no_comments), color = MehfilFlatColors.Muted, fontSize = 13.sp)
                }
                else -> CommentList(
                    comments = comments,
                    isLoadingMore = isLoadingMore,
                    hasMore = hasMore,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.weight(1f),
                )
            }
            MehfilSheetDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
private fun CommentInputRow(
    value: String,
    isPosting: Boolean,
    onValueChange: (String) -> Unit,
    onPost: () -> Unit,
) {
    val canSend = value.isNotBlank() && !isPosting
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MehfilInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(stringResource(R.string.mehfil_add_comment), fontSize = 13.sp, color = MehfilFlatColors.Muted) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )
        IconButton(
            onClick = { if (value.isNotBlank()) onPost() },
            enabled = canSend,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (canSend) MehfilFlatColors.Primary else MehfilFlatColors.Hairline.copy(alpha = 0.55f)),
        ) {
            if (isPosting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.mehfil_comment),
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

    MehfilSheet(
        onDismissRequest = { if (showComments && initialCommentTargetId == null) commentTargetId = null else onDismiss() },
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
            MehfilSheetEyebrow(stringResource(R.string.mehfil_sandesh))
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
                    stringResource(R.string.mehfil_comments),
                    fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                    color = MehfilFlatColors.Text,
                    modifier = Modifier.weight(1f),
                )
                if (!isLoading) Text("${comments.size}", fontSize = 13.sp, color = MehfilFlatColors.Muted)
            }
        }
        MehfilSheetDivider(modifier = Modifier.padding(horizontal = 16.dp))
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
        MehfilSheetDivider(modifier = Modifier.padding(horizontal = 16.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp).navigationBarsPadding().imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MehfilInput(
                value = commentText,
                onValueChange = onCommentTextChange,
                placeholder = { Text(stringResource(R.string.mehfil_add_comment), fontSize = 13.sp, color = MehfilFlatColors.Muted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                )
            IconButton(
                onClick = onPostComment,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) MehfilFlatColors.Primary else MehfilFlatColors.Hairline.copy(alpha = 0.55f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.mehfil_comment),
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
        MehfilSheetEyebrow(stringResource(R.string.mehfil_title))
        Text(
            stringResource(R.string.mehfil_sandesh_title),
            fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = MehfilFlatColors.Text,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            stringResource(R.string.mehfil_community_messages),
            fontSize = 12.sp,
            color = MehfilFlatColors.Muted,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        MehfilSheetDivider()
        sandeshes.forEachIndexed { index, sandesh ->
            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(sandesh.content, fontSize = 15.sp, lineHeight = 23.sp, color = MehfilFlatColors.Text)
                Text(formatTabletPostDate(sandesh.createdAt), fontSize = 12.sp, color = MehfilFlatColors.Muted)
                SandeshMedia(sandesh)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { onReact(sandesh.id) }, style = ButtonStyle.Ghost,
                        modifier = Modifier.heightIn(min = 48.dp), contentColor = MehfilFlatColors.Primary) {
                        Icon(if (sandesh.id in reactedSandeshIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(R.string.mehfil_like), modifier = Modifier.size(22.dp))
                        Text(" ${sandesh.reactionCount}")
                    }
                    Button(onClick = { onOpenComments(sandesh.id) }, style = ButtonStyle.Ghost,
                        modifier = Modifier.heightIn(min = 48.dp), contentColor = MehfilFlatColors.Primary) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(22.dp))
                        Text(stringResource(R.string.mehfil_comment_count, sandesh.commentCount))
                    }
                }
            }
            if (index < sandeshes.lastIndex) MehfilSheetDivider(alpha = .6f)
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
    inlinePreview: Boolean = false,
    initialContent: String = "",
) {
    val initialParts = remember(initialContent) { splitMehfilContent(initialContent) }
    var title by remember { mutableStateOf(initialParts.first.orEmpty()) }
    var content by remember { mutableStateOf(initialParts.second) }
    var space by remember { mutableStateOf(if (selectedSpace == "ALL") "REFLECTIVE" else selectedSpace) }
    var isAnonymous by remember { mutableStateOf(false) }
    val canPost = title.isNotBlank() && content.isNotBlank() && !isPosting

    val form: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MehfilSheetEyebrow(stringResource(R.string.mehfil_title))
            Text(
                stringResource(R.string.mehfil_new_post),
                fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = MehfilFlatColors.Text,
            )
            MehfilSheetDivider()
            MehfilInput(
                value = title,
                onValueChange = { title = it.replace('\n', ' ').replace('\r', ' ').take(180) },
                label = { Text(stringResource(R.string.mehfil_post_heading)) },
                placeholder = { Text(stringResource(R.string.mehfil_post_heading_hint)) },
                singleLine = true,
                enabled = !isPosting,
                modifier = Modifier.fillMaxWidth(),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                shape = RoundedCornerShape(12.dp),
                )
            MehfilInput(
                value = content,
                label = { Text(stringResource(R.string.mehfil_post_body)) },
                enabled = !isPosting,
                onValueChange = { content = it },
                placeholder = { Text(stringResource(R.string.mehfil_whats_on_mind), color = MehfilFlatColors.Muted) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).tutorialTarget("mehfil.write"),
                minLines = 4,
                shape = RoundedCornerShape(12.dp),
                )
            Column(Modifier.tutorialTarget("mehfil.share"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.mehfil_space), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MehfilFlatColors.Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("REFLECTIVE", "ACADEMIC").forEach { item ->
                        val selected = space == item
                        Button(onClick = { space = item }, enabled = !isPosting,
                            style = ButtonStyle.Ghost, shape = CircleShape,
                            modifier = Modifier.heightIn(min = 48.dp).background(
                                if (selected) MehfilFlatColors.Primary else MehfilFlatColors.Surface, CircleShape),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            contentColor = if (selected) Color.White else MehfilFlatColors.Text,
                        ) {
                            Text(
                                if (item == "REFLECTIVE") stringResource(R.string.mehfil_reflective) else stringResource(R.string.mehfil_academic),
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else MehfilFlatColors.Muted,
                            )
                        }
                    }
                }
                AnonymousPostToggle(isAnonymous = isAnonymous, enabled = !isPosting, onCheckedChange = { isAnonymous = it })
                com.composables.ui.components.Button(
                    onClick = { onPost(composeMehfilContent(title, content), space, isAnonymous) },
                    enabled = canPost,
                    style = com.composables.ui.components.ButtonStyle.Ghost,
                    shape = RoundedCornerShape(14.dp),
                    contentColor = if (canPost) Color.White else MehfilFlatColors.Muted,
                    modifier = Modifier.fillMaxWidth().background(
                        if (canPost) MehfilFlatColors.Primary else MehfilFlatColors.Hairline.copy(alpha = 0.55f),
                        RoundedCornerShape(14.dp)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        com.composables.ui.components.Text(
                            if (isAnonymous) stringResource(R.string.mehfil_post_anonymously) else stringResource(R.string.mehfil_share_post),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (canPost) Color.White else MehfilFlatColors.Muted,
                        )
                    }
                }
            }
        }
    }
    if (inlinePreview) form() else MehfilSheet(
        onDismissRequest = onDismiss,
    ) { form() }

}

@Composable
private fun AnonymousPostToggle(isAnonymous: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
            imageVector = ImageVector.vectorResource(R.drawable.ic_ghost),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isAnonymous) MehfilFlatColors.Primary else MehfilFlatColors.Muted,
        )
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.mehfil_hide_name), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MehfilFlatColors.Text)
            Text(stringResource(R.string.mehfil_name_hidden_help), fontSize = 11.sp, color = MehfilFlatColors.Muted)
        }
        Switch(
            checked = isAnonymous,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            accessibilityLabel = stringResource(R.string.mehfil_hide_name),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GuidelinesSheet(onDismiss: () -> Unit) {
    MehfilSheet(
        onDismissRequest = onDismiss,
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
            MehfilSheetEyebrow(stringResource(R.string.mehfil_title))
            Text(
                stringResource(R.string.mehfil_simple_rules),
                fontFamily = com.safarparmar.app.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                color = MehfilFlatColors.Text,
            )
            MehfilSheetDivider()
            GuidelineItem(R.drawable.ic_graduation_cap, stringResource(R.string.mehfil_rule_study_title), stringResource(R.string.mehfil_rule_study_body))
            GuidelineItem(R.drawable.ic_chat, stringResource(R.string.mehfil_rule_talk_title), stringResource(R.string.mehfil_rule_talk_body))
            GuidelineItem(R.drawable.ic_shield_check, stringResource(R.string.mehfil_rule_kind_title), stringResource(R.string.mehfil_rule_kind_body))
            GuidelineItem(R.drawable.ic_ghost, stringResource(R.string.mehfil_rule_safe_title), stringResource(R.string.mehfil_rule_safe_body))
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
                imageVector = ImageVector.vectorResource(iconRes),
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

/** Composables UI sheet, using the app's density-safe popup bridge. */
@Composable
private fun MehfilSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val state = rememberBottomSheetState(detents = listOf(BottomSheetDetent.Hidden, BottomSheetDetent.FullyExpanded))
    LaunchedEffect(Unit) { state.targetDetent = BottomSheetDetent.FullyExpanded }
    SafarComposablesBottomSheet(state = state, onDismissRequest = onDismissRequest,
        backgroundColor = MehfilFlatColors.ComponentBackground, contentColor = MehfilFlatColors.Text,
        contentPadding = PaddingValues(top = 16.dp), body = content)
}

/** Keeps the existing screen-owned input contract while using the library's state-based field. */
@Composable
private fun MehfilInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, singleLine: Boolean = false, minLines: Int = 1,
    label: (@Composable () -> Unit)? = null, placeholder: (@Composable () -> Unit)? = null,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    textStyle: androidx.compose.ui.text.TextStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp),
) {
    val state = rememberTextFieldState(value)
    val callback by rememberUpdatedState(onValueChange)
    val latestValue by rememberUpdatedState(value)
    LaunchedEffect(value) { if (state.text.toString() != value) state.edit { replace(0, length, value) } }
    LaunchedEffect(state) { snapshotFlow { state.text.toString() }.collect { if (it != latestValue) callback(it) } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.invoke()
        com.composables.ui.components.TextField(state = state, enabled = enabled,
            modifier = Modifier.fillMaxWidth(), shape = shape, placeholder = placeholder,
            backgroundColor = MehfilFlatColors.Surface, contentColor = MehfilFlatColors.Text,
            borderColor = MehfilFlatColors.Hairline, placeholderColor = MehfilFlatColors.Muted,
            textStyle = textStyle, contentPadding = PaddingValues(12.dp),
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(minHeightInLines = minLines))
    }
}

@Composable
private fun MehfilSheetEyebrow(text: String) {
    Text(text, color = MehfilFlatColors.Primary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
}

@Composable
private fun MehfilSheetDivider(modifier: Modifier = Modifier, alpha: Float = 1f) {
    com.composables.ui.components.HorizontalSeparator(modifier = modifier, color = MehfilFlatColors.Hairline.copy(alpha = alpha))
}
