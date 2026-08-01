package com.safarparmar.app.ui.home

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.domain.model.AnnouncementType
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import com.safarparmar.app.ui.mehfil.formatPostDate
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.util.YoutubeUrls

private val URL_IN_TEXT = Regex("""https?://[^\s<>"']+""")

/** Flat hairline ink for the Updates sheet (mirrors EkagraInk, theme-adaptive). */
private data class UpdatesInk(
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val hairline: Color,
)

@Composable
private fun rememberUpdatesInk(): UpdatesInk {
    val scheme = MaterialTheme.colorScheme
    return UpdatesInk(
        primaryText = scheme.onSurface,
        secondaryText = scheme.onSurfaceVariant,
        mutedText = scheme.onSurfaceVariant.copy(alpha = 0.72f),
        hairline = scheme.outlineVariant.copy(alpha = 0.65f),
    )
}

internal enum class UpdatesFilter {
    ALL,
    ANNOUNCEMENTS,
    UPDATES,
}

internal fun NotificationFeedItem.matchesFilter(filter: UpdatesFilter): Boolean =
    when (filter) {
        UpdatesFilter.ALL -> true
        UpdatesFilter.ANNOUNCEMENTS ->
            type == AnnouncementType.GENERAL || type == AnnouncementType.MAINTENANCE
        UpdatesFilter.UPDATES -> type == AnnouncementType.APP_UPDATE
    }

internal data class AnnouncementLinkInfo(
    val displayBody: String,
    val youtubeVideoId: String?,
    val youtubeUrl: String?,
    val webUrl: String?,
)

internal fun parseAnnouncementLinks(body: String, deepLink: String?): AnnouncementLinkInfo {
    fun cleanUrl(raw: String): String =
        raw.trimEnd('.', ',', ')', ']', '!', '?', ';', '"', '\'')

    val urlsFromBody = URL_IN_TEXT.findAll(body).map { cleanUrl(it.value) }.toList()
    val candidates = buildList {
        addAll(urlsFromBody)
        deepLink
            ?.trim()
            ?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?.let { add(cleanUrl(it)) }
    }.distinct()

    val youtube = candidates.firstNotNullOfOrNull { url ->
        YoutubeUrls.extractVideoId(url)?.let { id -> id to YoutubeUrls.watchUrl(id) }
    }
    val webUrl = candidates.firstOrNull { YoutubeUrls.extractVideoId(it) == null }
    // Preserve author line breaks. Only collapse leftover horizontal spaces
    // after URL stripping (e.g. "  " left where a link was removed).
    val displayBody = body
        .replace(URL_IN_TEXT) { "" }
        .replace(Regex("""[^\S\r\n]{2,}"""), " ")
        .replace(Regex("""\r\n?"""), "\n")
        .trim()

    return AnnouncementLinkInfo(
        displayBody = displayBody,
        youtubeVideoId = youtube?.first,
        youtubeUrl = youtube?.second,
        webUrl = webUrl,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsBottomSheet(
    items: List<NotificationFeedItem>,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onMarkAllAsRead: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDismissAnnouncement: (String) -> Unit,
    onAnnouncementAction: (NotificationFeedItem) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        AnnouncementsSheetContent(
            items = items,
            isLoading = isLoading,
            onMarkAllAsRead = onMarkAllAsRead,
            onMarkAsRead = onMarkAsRead,
            onDismissAnnouncement = onDismissAnnouncement,
            onAnnouncementAction = onAnnouncementAction,
            modifier = Modifier
                .fillMaxWidth()
                // A tall measured sheet gives Material 3 a partially-expanded anchor.
                .fillMaxHeight(0.90f),
        )
    }
}

@Composable
private fun AnnouncementsSheetContent(
    items: List<NotificationFeedItem>,
    isLoading: Boolean,
    onMarkAllAsRead: () -> Unit,
    onMarkAsRead: (String) -> Unit,
    onDismissAnnouncement: (String) -> Unit,
    onAnnouncementAction: (NotificationFeedItem) -> Unit,
    modifier: Modifier = Modifier,
    initialFilter: UpdatesFilter = UpdatesFilter.ALL,
) {
    val ink = rememberUpdatesInk()
    val accent = MaterialTheme.colorScheme.primary
    var selectedFilter by remember { mutableStateOf(initialFilter) }

    val playingAudioItemId = remember { mutableStateOf<String?>(null) }
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }

    fun releaseAudio() {
        val player = mediaPlayer.value
        mediaPlayer.value = null
        playingAudioItemId.value = null
        player?.let {
            kotlin.concurrent.thread {
                runCatching { it.stop() }
                runCatching { it.release() }
            }
        }
    }

    fun toggleAudio(itemId: String, url: String) {
        if (playingAudioItemId.value == itemId) {
            releaseAudio()
            return
        }
        releaseAudio()
        playingAudioItemId.value = itemId
        try {
            mediaPlayer.value = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                )
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
                setOnCompletionListener { releaseAudio() }
                setOnErrorListener { _, _, _ ->
                    releaseAudio()
                    true
                }
            }
        } catch (_: Exception) {
            releaseAudio()
        }
    }

    DisposableEffect(Unit) {
        onDispose { releaseAudio() }
    }

    val filteredItems = remember(items, selectedFilter) {
        items.filter { it.matchesFilter(selectedFilter) }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "UPDATES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = ink.secondaryText,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "✕ hides an item. Mark read keeps it.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ink.mutedText,
                )
            }
            Text(
                text = "Mark all as read",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (items.any(NotificationFeedItem::isUnread)) {
                    accent
                } else {
                    ink.mutedText
                },
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        enabled = items.any(NotificationFeedItem::isUnread),
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onMarkAllAsRead,
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        UpdatesHairline(ink.hairline)

        UpdatesFilterChips(
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            ink = ink,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )

        UpdatesHairline(ink.hairline)

        when {
            isLoading -> LoadingState(ink = ink, modifier = Modifier.weight(1f))
            items.isEmpty() -> EmptyAnnouncementsState(
                message = "You're all caught up!",
                ink = ink,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            filteredItems.isEmpty() -> EmptyAnnouncementsState(
                message = emptyMessageFor(selectedFilter),
                ink = ink,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                itemsIndexed(
                    items = filteredItems,
                    key = { _, item -> item.id },
                ) { index, item ->
                    AnnouncementRow(
                        item = item,
                        ink = ink,
                        accent = accent,
                        isAudioPlaying = playingAudioItemId.value == item.id,
                        onMarkAsRead = { onMarkAsRead(item.id) },
                        onDismiss = { onDismissAnnouncement(item.id) },
                        onAction = { onAnnouncementAction(item) },
                        onToggleAudio = { url -> toggleAudio(item.id, url) },
                    )
                    if (index < filteredItems.lastIndex) {
                        UpdatesHairline(
                            color = ink.hairline,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

private fun emptyMessageFor(filter: UpdatesFilter): String =
    when (filter) {
        UpdatesFilter.ALL -> "You're all caught up!"
        UpdatesFilter.ANNOUNCEMENTS -> "No announcements yet"
        UpdatesFilter.UPDATES -> "No updates yet"
    }

@Composable
private fun UpdatesHairline(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
private fun UpdatesFilterChips(
    selected: UpdatesFilter,
    onSelect: (UpdatesFilter) -> Unit,
    ink: UpdatesInk,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        UpdatesFilter.entries.forEach { option ->
            val isSelected = option == selected
            val label = when (option) {
                UpdatesFilter.ALL -> "All"
                UpdatesFilter.ANNOUNCEMENTS -> "Announcements"
                UpdatesFilter.UPDATES -> "Updates"
            }
            val borderColor = if (isSelected) accent else ink.hairline
            val textColor = if (isSelected) accent else ink.mutedText
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, borderColor, RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = textColor,
                )
            }
        }
    }
}

@Composable
private fun AnnouncementRow(
    item: NotificationFeedItem,
    ink: UpdatesInk,
    accent: Color,
    isAudioPlaying: Boolean,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onToggleAudio: (String) -> Unit,
) {
    val context = LocalContext.current
    val isUpdate = item.type == AnnouncementType.APP_UPDATE
    val titleColor = if (item.isUnread) ink.primaryText else ink.mutedText
    val bodyColor = if (item.isUnread) ink.secondaryText else ink.mutedText
    val links = remember(item.body, item.deepLink) {
        parseAnnouncementLinks(item.body, item.deepLink)
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun onRowClick() {
        if (isUpdate) {
            onAction()
        } else {
            onMarkAsRead()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::onRowClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            AnnouncementTypeIcon(
                type = item.type,
                ink = ink,
                accent = accent,
                muted = !item.isUnread,
            )
            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }

                if (links.displayBody.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = links.displayBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bodyColor,
                    )
                }

                if (isUpdate) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Open Play Store",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accent.copy(alpha = if (item.isUnread) 1f else 0.7f),
                    )
                }

                links.youtubeVideoId?.let { videoId ->
                    Spacer(Modifier.height(10.dp))
                    YoutubeThumb(
                        videoId = videoId,
                        ink = ink,
                        onClick = {
                            onMarkAsRead()
                            openUrl(links.youtubeUrl ?: YoutubeUrls.watchUrl(videoId))
                        },
                    )
                }

                if (links.youtubeVideoId == null && !links.webUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    LinkChip(
                        url = links.webUrl,
                        ink = ink,
                        accent = accent,
                        onClick = {
                            onMarkAsRead()
                            openUrl(links.webUrl)
                        },
                    )
                }

                item.audioUrl?.takeIf { it.isNotBlank() }?.let { audioUrl ->
                    Spacer(Modifier.height(10.dp))
                    AudioPlayRow(
                        isPlaying = isAudioPlaying,
                        ink = ink,
                        accent = accent,
                        onToggle = {
                            onMarkAsRead()
                            onToggleAudio(audioUrl)
                        },
                    )
                }

                if (item.createdAt.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(
                        text = formatPostDate(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = ink.mutedText,
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Hide ${item.title}",
                    tint = ink.mutedText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun YoutubeThumb(
    videoId: String,
    ink: UpdatesInk,
    onClick: () -> Unit,
) {
    val thumbnails = remember(videoId) { YoutubeUrls.thumbnailUrls(videoId) }
    var thumbIndex by remember(videoId) { mutableIntStateOf(0) }
    val thumbUrl = thumbnails.getOrElse(thumbIndex) { "" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ink.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbUrl.isNotBlank()) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = "YouTube video",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    if (thumbIndex < thumbnails.lastIndex) {
                        thumbIndex += 1
                    }
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ink.hairline.copy(alpha = 0.35f)),
            )
        }
        Box(modifier = Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.28f)))
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Open YouTube",
            tint = Color.White,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.55f), CircleShape)
                .padding(8.dp),
        )
    }
}

@Composable
private fun LinkChip(
    url: String,
    ink: UpdatesInk,
    accent: Color,
    onClick: () -> Unit,
) {
    val host = remember(url) {
        runCatching { Uri.parse(url).host }.getOrNull().orEmpty().ifBlank { "Open link" }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, ink.hairline, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.Link,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = host,
            style = MaterialTheme.typography.labelMedium,
            color = ink.primaryText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AudioPlayRow(
    isPlaying: Boolean,
    ink: UpdatesInk,
    accent: Color,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, if (isPlaying) accent else ink.hairline, RoundedCornerShape(6.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "Pause audio" else "Play audio",
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isPlaying) "Playing audio…" else "Play audio",
            style = MaterialTheme.typography.labelLarge,
            color = ink.primaryText,
        )
    }
}

@Composable
private fun AnnouncementTypeIcon(
    type: AnnouncementType,
    ink: UpdatesInk,
    accent: Color,
    muted: Boolean,
) {
    val icon: ImageVector
    val description: String
    val tint = when (type) {
        AnnouncementType.APP_UPDATE -> {
            icon = Icons.Outlined.NewReleases
            description = "App update"
            accent
        }
        AnnouncementType.MAINTENANCE -> {
            icon = Icons.Outlined.WarningAmber
            description = "Maintenance alert"
            MaterialTheme.colorScheme.error
        }
        AnnouncementType.GENERAL -> {
            icon = Icons.Outlined.Campaign
            description = "General announcement"
            ink.secondaryText
        }
    }
    val alpha = if (muted) 0.55f else 1f

    Box(
        modifier = Modifier
            .size(36.dp)
            .border(1.dp, ink.hairline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint.copy(alpha = alpha),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LoadingState(
    ink: UpdatesInk,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = ink.secondaryText,
            strokeWidth = 2.dp,
        )
    }
}

@Composable
private fun EmptyAnnouncementsState(
    message: String,
    ink: UpdatesInk,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.dp, ink.hairline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ink.primaryText,
        )
    }
}

private val previewAnnouncements = listOf(
    NotificationFeedItem(
        id = "update",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.APP_UPDATE,
        title = "New Version Available!",
        body = "A faster dashboard, improved reminders, and several reliability fixes are ready.",
        createdAt = "2026-07-10T08:00:00Z",
        isUnread = true,
    ),
    NotificationFeedItem(
        id = "maintenance",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.MAINTENANCE,
        title = "Scheduled maintenance",
        body = "Brief downtime tonight at 12:00 AM IST.",
        createdAt = "2026-07-09T20:00:00Z",
        isUnread = true,
    ),
    NotificationFeedItem(
        id = "yt",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.GENERAL,
        title = "Watch this",
        body = "New tip video https://youtu.be/i65MjKQCWUE",
        createdAt = "2026-07-09T18:00:00Z",
        isUnread = true,
        audioUrl = "https://example.com/clip.mp3",
    ),
    NotificationFeedItem(
        id = "general",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.GENERAL,
        title = "A fresh new dashboard",
        body = "Welcome to our newly redesigned dashboard.",
        createdAt = "2026-07-08T10:30:00Z",
        isUnread = false,
        deepLink = "https://safar.parmarssc.in/updates",
    ),
)

@Preview(name = "Updates - Light", showBackground = true, heightDp = 720)
@Composable
private fun AnnouncementsSheetLightPreview() {
    SafarTheme(darkTheme = false) {
        Surface {
            AnnouncementsSheetContent(
                items = previewAnnouncements,
                isLoading = false,
                onMarkAllAsRead = {},
                onMarkAsRead = {},
                onDismissAnnouncement = {},
                onAnnouncementAction = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Updates - Dark", showBackground = true, heightDp = 720)
@Composable
private fun AnnouncementsSheetDarkPreview() {
    SafarTheme(darkTheme = true) {
        Surface {
            AnnouncementsSheetContent(
                items = previewAnnouncements,
                isLoading = false,
                onMarkAllAsRead = {},
                onMarkAsRead = {},
                onDismissAnnouncement = {},
                onAnnouncementAction = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Updates - Empty", showBackground = true, heightDp = 540)
@Composable
private fun AnnouncementsSheetEmptyPreview() {
    SafarTheme {
        Surface {
            AnnouncementsSheetContent(
                items = emptyList(),
                isLoading = false,
                onMarkAllAsRead = {},
                onMarkAsRead = {},
                onDismissAnnouncement = {},
                onAnnouncementAction = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Preview(name = "Updates - Updates chip empty", showBackground = true, heightDp = 540)
@Composable
private fun AnnouncementsSheetUpdatesFilterEmptyPreview() {
    SafarTheme(darkTheme = false) {
        Surface {
            AnnouncementsSheetContent(
                items = previewAnnouncements.filter {
                    it.type != AnnouncementType.APP_UPDATE
                },
                isLoading = false,
                onMarkAllAsRead = {},
                onMarkAsRead = {},
                onDismissAnnouncement = {},
                onAnnouncementAction = {},
                modifier = Modifier.fillMaxSize(),
                initialFilter = UpdatesFilter.UPDATES,
            )
        }
    }
}
