package com.safarparmar.app.ui.mehfil

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.composables.ui.components.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.composables.ui.components.Button
import com.composables.ui.components.ButtonStyle
import com.composables.ui.components.DropdownMenuAlignment
import com.composables.ui.components.DropdownMenuItem
import com.composables.ui.components.DropdownMenuPanel
import com.composables.ui.components.IconButton
import com.composables.ui.components.Text as MenuText
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.MehfilPost
import com.safarparmar.app.domain.model.Sandesh
import com.safarparmar.app.ui.components.popups.SafarComposablesDropdownMenu
import com.safarparmar.app.ui.tutorial.tutorialTarget
import java.util.Locale

/** Only layout/chrome decisions use this flag; card styling is shared across form factors. */
internal val LocalMehfilTabletStyle = staticCompositionLocalOf { false }
private val LocalMehfilDualPane = staticCompositionLocalOf { false }

/** Reference proportions: a broad community feed and a separate, independently scrolling Sandesh rail. */
@Composable
internal fun MehfilFeedLayout(
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String>,
    onReact: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    feed: @Composable (Boolean) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val expanded = LocalMehfilTabletStyle.current && maxWidth >= 760.dp
        if (!expanded) {
            Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp)
                .background(MehfilFlatColors.CommunityPanel, RoundedCornerShape(20.dp)).padding(vertical = 14.dp)) { feed(false) }
        } else {
            CompositionLocalProvider(LocalMehfilDualPane provides true) {
            Row(
                Modifier.fillMaxSize().padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(Modifier.weight(0.595f).fillMaxHeight()
                    .background(MehfilFlatColors.CommunityPanel, RoundedCornerShape(20.dp)).padding(20.dp)) { feed(true) }
                MehfilSandeshRail(
                    sandeshes, reactedSandeshIds, onReact, onCommentClick,
                    Modifier.weight(0.405f).fillMaxHeight().background(MehfilFlatColors.SandeshPanel, RoundedCornerShape(20.dp)).padding(18.dp),
                )
            }
            }
        }
    }
}

@Composable
private fun MehfilSandeshRail(
    sandeshes: List<Sandesh>,
    reactedSandeshIds: Set<String>,
    onReact: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.mehfil_sandesh).uppercase(Locale.getDefault()), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.SandeshAccent)
        Text(stringResource(R.string.mehfil_official_description), fontSize = 16.sp, color = MehfilFlatColors.Muted)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(4.dp)) {
            items(sandeshes, key = { it.id }) { sandesh ->
                SandeshAnnouncementCard(sandesh, sandesh.id in reactedSandeshIds, onReact, onCommentClick)
            }
        }
    }
}

/** Uses the library button's keyboard/focus/press behaviour, with the reference's filled rectangular tabs. */
@Composable
internal fun MehfilRoomSelector(selectedSpace: String, onJoinRoom: (String) -> Unit) {
    val rooms = listOf("ALL" to R.string.common_all, "ACADEMIC" to R.string.mehfil_academic, "REFLECTIVE" to R.string.mehfil_reflective)
    Row(Modifier.fillMaxWidth().padding(horizontal = if (LocalMehfilDualPane.current) 0.dp else 16.dp).tutorialTarget("mehfil.space"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        rooms.forEach { (key, label) ->
            val active = selectedSpace.equals(key, true)
            val shape = CircleShape
            Button(
                onClick = { onJoinRoom(key) }, style = ButtonStyle.Ghost, shape = shape,
                modifier = Modifier.weight(if (key == "ALL") 0.8f else 1f, fill = false)
                    .height(40.dp)
                    .background(if (active) MehfilFlatColors.Primary else MehfilFlatColors.Surface, shape)
                    .border(1.dp, if (active) MehfilFlatColors.Primary else MehfilFlatColors.Hairline, shape)
                    .semantics { selected = active; role = Role.Tab },
                contentPadding = PaddingValues(horizontal = 20.dp),
                contentColor = if (active) Color.White else MehfilFlatColors.Text,
            ) {
                MenuText(stringResource(label).lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun MehfilReferencePostCard(
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
    val locked = canConnect && !mehfilDm && !isLoadingPremiumFeatures
    BoxWithConstraints {
        val roomy = maxWidth >= 480.dp
        val showConnectLabel = maxWidth >= 360.dp
        Column(
            Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp), clip = false, ambientColor = Color(0x14642BA4), spotColor = Color(0x14642BA4))
                .tutorialTarget("mehfil.react")
                .clip(RoundedCornerShape(14.dp)).background(MehfilFlatColors.Surface)
                .border(0.8.dp, MehfilFlatColors.Hairline, RoundedCornerShape(14.dp))
                .padding(if (roomy) 22.dp else 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (roomy) 16.dp else 8.dp)) {
                MehfilAvatar(post.authorName, post.authorAvatar, if (roomy) 58 else 44)
                Column(Modifier.then(if (roomy) Modifier.widthIn(max = 170.dp) else Modifier.weight(1f)), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(post.authorName, fontSize = if (roomy) 17.sp else 14.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatTabletPostDate(post.createdAt), fontSize = if (roomy) 14.sp else 11.sp, color = MehfilFlatColors.Muted, maxLines = 1)
                }
                if (post.space.isNotBlank()) MehfilSpaceBadge(post.space, compact = !roomy)
                if (roomy) Spacer(Modifier.weight(1f))
                MehfilCardMenu(onComment, onSave, isSaved)
            }
            Spacer(Modifier.height(if (roomy) 24.dp else 16.dp))
            MehfilPostText(post.content, roomy)
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (roomy) 20.dp else 4.dp)) {
                MehfilCardAction(MehfilCardGlyph.Heart, post.reactionCount.toString(), stringResource(R.string.mehfil_like), onLike, filled = post.userLiked)
                MehfilCardAction(MehfilCardGlyph.Comment, post.commentCount.toString(), stringResource(R.string.mehfil_comment), onComment)
                Spacer(Modifier.weight(1f))
                if (canConnect) MehfilCardAction(
                    if (locked) MehfilCardGlyph.Lock else MehfilCardGlyph.Connect,
                    if (showConnectLabel) stringResource(R.string.mehfil_connect) else null, stringResource(R.string.mehfil_connect), onConnect,
                    if (locked) MehfilFlatColors.Muted else MehfilFlatColors.Primary,
                )
                MehfilCardAction(MehfilCardGlyph.Bookmark, if (roomy) stringResource(R.string.common_save) else null, stringResource(R.string.common_save), onSave, color = MehfilFlatColors.Primary, filled = isSaved)
            }
        }
    }
}

@Composable
private fun MehfilAvatar(name: String, avatarUrl: String?, diameter: Int) {
    Box(Modifier.size(diameter.dp).clip(CircleShape).background(MehfilFlatColors.Lavender), contentAlignment = Alignment.Center) {
        Text(name.firstOrNull()?.uppercase() ?: "?", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = MehfilFlatColors.Primary)
        if (!avatarUrl.isNullOrBlank()) AsyncImage(model = avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun MehfilSpaceBadge(space: String, compact: Boolean = false) {
    Row(Modifier.background(if (space.equals("ACADEMIC", true)) MehfilFlatColors.AcademicBadge else MehfilFlatColors.ReflectiveBadge, CircleShape).padding(horizontal = if (compact) 8.dp else 12.dp, vertical = if (compact) 6.dp else 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)) {
        val label = when (space.uppercase(Locale.ROOT)) {
            "ACADEMIC" -> stringResource(R.string.mehfil_academic)
            "REFLECTIVE" -> stringResource(R.string.mehfil_reflective)
            else -> space
        }.lowercase(Locale.getDefault()).replaceFirstChar { it.titlecase(Locale.getDefault()) }
        Text(label, fontSize = if (compact) 11.sp else 14.sp, color = MehfilFlatColors.Primary, maxLines = 1)
    }
}

/** The API accepts content only; preserve heading/body across feed, saved posts, and comments. */
internal fun composeMehfilContent(title: String, body: String): String =
    title.trim().replace('\n', ' ').replace('\r', ' ').take(180) + "\n\n" + body.trim()

/** A leading paragraph/line acts as a title only when the post actually includes a body. */
internal fun splitMehfilContent(content: String): Pair<String?, String> {
    val trimmed = content.trim()
    val lineBreak = trimmed.indexOf('\n')
    return if (lineBreak in 1..180 && trimmed.substring(lineBreak).isNotBlank()) {
        trimmed.substring(0, lineBreak).trim() to trimmed.substring(lineBreak).trim()
    } else null to trimmed
}

@Composable
private fun MehfilPostText(content: String, roomy: Boolean, announcement: Boolean = false) {
    val (title, body) = remember(content) { splitMehfilContent(content) }
    if (title != null) {
        Text(title, fontSize = if (announcement) 20.sp else if (roomy) 24.sp else 20.sp, lineHeight = if (roomy) 30.sp else 26.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.Text)
        Spacer(Modifier.height(12.dp))
    }
    Text(body, fontSize = 16.sp, lineHeight = 24.sp, color = MehfilFlatColors.Muted, letterSpacing = 0.sp)
}

@Composable
internal fun MehfilAnnouncementCard(sandesh: Sandesh, reacted: Boolean, onReact: (String) -> Unit, onComment: (String) -> Unit, media: @Composable () -> Unit) {
    val parts = remember(sandesh.content) { splitMehfilContent(sandesh.content) }
    var expanded by rememberSaveable(sandesh.id) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(16.dp), clip = false,
            ambientColor = Color(0x146B422B), spotColor = Color(0x146B422B))
            .clip(RoundedCornerShape(16.dp)).background(MehfilFlatColors.Surface).padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(54.dp).background(MehfilFlatColors.SandeshPanel, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(28.dp), tint = MehfilFlatColors.Text)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(parts.first ?: stringResource(R.string.mehfil_parmar_corner), fontSize = 19.sp, fontWeight = FontWeight.Bold, color = MehfilFlatColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(formatTabletPostDate(sandesh.createdAt), fontSize = 14.sp, color = MehfilFlatColors.Muted)
            }
            MehfilCardMenu({ onComment(sandesh.id) })
        }
        Spacer(Modifier.height(16.dp))
        Text(parts.second, fontSize = 16.sp, lineHeight = 24.sp, color = MehfilFlatColors.Muted,
            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis)
        if (expanded) {
            media()
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                MehfilCardAction(MehfilCardGlyph.Heart, sandesh.reactionCount.toString(), stringResource(R.string.mehfil_like), { onReact(sandesh.id) }, filled = reacted)
                MehfilCardAction(MehfilCardGlyph.Comment, sandesh.commentCount.toString(), stringResource(R.string.mehfil_comment), { onComment(sandesh.id) })
            }
        }
        Spacer(Modifier.height(14.dp))
        androidx.compose.material3.HorizontalDivider(color = MehfilFlatColors.Hairline)
        Spacer(Modifier.height(10.dp))
        Button(onClick = { expanded = !expanded }, style = ButtonStyle.Ghost,
            modifier = Modifier.fillMaxWidth().background(MehfilFlatColors.SandeshPanel, RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), contentColor = MehfilFlatColors.Text) {
            MenuText(stringResource(if (expanded) R.string.common_collapse else R.string.mehfil_view_details), fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun MehfilCardAction(icon: MehfilCardGlyph, label: String?, description: String, onClick: () -> Unit, color: Color = MehfilFlatColors.Muted, filled: Boolean = false) {
    Button(onClick = onClick, style = ButtonStyle.Ghost, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp), contentColor = color, modifier = Modifier.heightIn(min = 48.dp).then(
        if (icon == MehfilCardGlyph.Connect || icon == MehfilCardGlyph.Lock)
            Modifier.border(1.dp, MehfilFlatColors.Primary, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp)
        else Modifier)) {
        MehfilCardArtwork(icon, Modifier.size(28.dp), color = color, filled = filled, description = description)
        if (label != null) {
            Spacer(Modifier.width(8.dp))
            MenuText(label, fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif, color = if (icon == MehfilCardGlyph.Heart) MehfilFlatColors.Muted else color, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/** Composables UI anchored overflow menu, using the app's bounded popup wrapper. */
@Composable
private fun MehfilCardMenu(onComment: () -> Unit, onSave: (() -> Unit)? = null, saved: Boolean = false) {
    var expanded by remember { mutableStateOf(false) }
    SafarComposablesDropdownMenu(
        expanded = expanded, onExpandedChange = { expanded = it }, alignment = DropdownMenuAlignment.End,
        panel = {
            DropdownMenuPanel {
                DropdownMenuItem(onClick = { expanded = false; onComment() }) { MenuText(stringResource(R.string.mehfil_comment)) }
                if (onSave != null) DropdownMenuItem(onClick = { expanded = false; onSave() }) { MenuText(stringResource(if (saved) R.string.mehfil_unsave_post else R.string.common_save)) }
            }
        },
    ) {
        IconButton(onClick = { expanded = !expanded }, style = ButtonStyle.Ghost, modifier = Modifier.size(40.dp)) {
            MehfilCardArtwork(MehfilCardGlyph.More, Modifier.size(24.dp), description = stringResource(R.string.mehfil_post_options))
        }
    }
}

/** Reference-style relative dates, localized for both phone and tablet cards. */
@Composable
internal fun formatTabletPostDate(timestamp: String): String {
    val days = runCatching { java.time.Duration.between(java.time.ZonedDateTime.parse(timestamp).toInstant(), java.time.Instant.now()).toDays() }.getOrDefault(0L)
    if (days in 1..6) return androidx.compose.ui.res.pluralStringResource(R.plurals.mehfil_days_ago, days.toInt(), days.toInt())
    return runCatching {
        android.text.format.DateUtils.getRelativeTimeSpanString(
            java.time.ZonedDateTime.parse(timestamp).toInstant().toEpochMilli(),
            System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    }.getOrElse { formatPostDate(timestamp) }
}
