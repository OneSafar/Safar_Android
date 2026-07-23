package com.safarparmar.app.ui.home

import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import coil.compose.AsyncImage
import com.safarparmar.app.domain.model.AnnouncementType
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import com.safarparmar.app.ui.mehfil.formatPostDate
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.util.YoutubeUrls

private val URL_IN_TEXT = Regex("""https?://[^\s<>"']+""")

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
    val displayBody = body
        .replace(URL_IN_TEXT) { "" }
        .replace(Regex("""\s{2,}"""), " ")
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
) {
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

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Updates",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Mark read keeps items. ✕ hides them.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onMarkAllAsRead,
                enabled = items.any(NotificationFeedItem::isUnread),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text("Mark all as read")
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            isLoading -> LoadingState(modifier = Modifier.weight(1f))
            items.isEmpty() -> EmptyAnnouncementsState(modifier = Modifier.weight(1f))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    items = items,
                    key = NotificationFeedItem::id,
                ) { item ->
                    AnnouncementRow(
                        item = item,
                        isAudioPlaying = playingAudioItemId.value == item.id,
                        onClick = { onMarkAsRead(item.id) },
                        onDismiss = { onDismissAnnouncement(item.id) },
                        onAction = { onAnnouncementAction(item) },
                        onToggleAudio = { url -> toggleAudio(item.id, url) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementRow(
    item: NotificationFeedItem,
    isAudioPlaying: Boolean,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onToggleAudio: (String) -> Unit,
) {
    val context = LocalContext.current
    val contentAlpha = if (item.isUnread) 1f else 0.62f
    val containerColor = if (item.isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val links = remember(item.body, item.deepLink) {
        parseAnnouncementLinks(item.body, item.deepLink)
    }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                AnnouncementTypeIcon(type = item.type, alpha = contentAlpha)
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
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }

                    if (links.displayBody.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = links.displayBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        )
                    }

                    links.youtubeVideoId?.let { videoId ->
                        Spacer(Modifier.height(10.dp))
                        YoutubeThumb(
                            videoId = videoId,
                            contentAlpha = contentAlpha,
                            onClick = {
                                onClick()
                                openUrl(links.youtubeUrl ?: YoutubeUrls.watchUrl(videoId))
                            },
                        )
                    }

                    if (links.youtubeVideoId == null && !links.webUrl.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        LinkChip(
                            url = links.webUrl,
                            contentAlpha = contentAlpha,
                            onClick = {
                                onClick()
                                openUrl(links.webUrl)
                            },
                        )
                    }

                    item.audioUrl?.takeIf { it.isNotBlank() }?.let { audioUrl ->
                        Spacer(Modifier.height(10.dp))
                        AudioPlayRow(
                            isPlaying = isAudioPlaying,
                            contentAlpha = contentAlpha,
                            onToggle = {
                                onClick()
                                onToggleAudio(audioUrl)
                            },
                        )
                    }

                    if (item.createdAt.isNotBlank()) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = formatPostDate(item.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha * 0.8f),
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Hide ${item.title}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item.type == AnnouncementType.APP_UPDATE) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.padding(start = 48.dp, top = 10.dp),
                ) {
                    Text("Update Now")
                }
            }
        }
    }
}

@Composable
private fun YoutubeThumb(
    videoId: String,
    contentAlpha: Float,
    onClick: () -> Unit,
) {
    val thumbnails = remember(videoId) { YoutubeUrls.thumbnailUrls(videoId) }
    var thumbIndex by remember(videoId) { mutableIntStateOf(0) }
    val thumbUrl = thumbnails.getOrElse(thumbIndex) { "" }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .clip(RoundedCornerShape(12.dp))
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
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha)),
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
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(8.dp),
        )
    }
}

@Composable
private fun LinkChip(
    url: String,
    contentAlpha: Float,
    onClick: () -> Unit,
) {
    val host = remember(url) {
        runCatching { Uri.parse(url).host }.getOrNull().orEmpty().ifBlank { "Open link" }
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f * contentAlpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = host,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AudioPlayRow(
    isPlaying: Boolean,
    contentAlpha: Float,
    onToggle: () -> Unit,
) {
    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f * contentAlpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause audio" else "Play audio",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isPlaying) "Playing audio…" else "Play audio",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            )
        }
    }
}

@Composable
private fun AnnouncementTypeIcon(type: AnnouncementType, alpha: Float) {
    val icon: ImageVector
    val description: String
    val tint = when (type) {
        AnnouncementType.APP_UPDATE -> {
            icon = Icons.Outlined.NewReleases
            description = "App update"
            MaterialTheme.colorScheme.primary
        }
        AnnouncementType.MAINTENANCE -> {
            icon = Icons.Outlined.WarningAmber
            description = "Maintenance alert"
            MaterialTheme.colorScheme.error
        }
        AnnouncementType.GENERAL -> {
            icon = Icons.Outlined.Campaign
            description = "General announcement"
            MaterialTheme.colorScheme.tertiary
        }
    }

    Surface(
        modifier = Modifier.size(36.dp),
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint.copy(alpha = alpha),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun EmptyAnnouncementsState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ) {
            Icon(
                imageVector = Icons.Outlined.DoneAll,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(18.dp).size(36.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
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

@Preview(name = "Announcements - Light", showBackground = true, heightDp = 720)
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

@Preview(name = "Announcements - Dark", showBackground = true, heightDp = 720)
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

@Preview(name = "Announcements - Empty", showBackground = true, heightDp = 540)
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
