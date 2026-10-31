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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import com.safarparmar.app.ui.components.popups.SafarModalBottomSheet as ModalBottomSheet
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safarparmar.app.R
import com.safarparmar.app.domain.model.AnnouncementType
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import com.safarparmar.app.ui.mehfil.formatPostDate
import com.safarparmar.app.ui.theme.SafarTheme
import com.safarparmar.app.util.YoutubeUrls

private val NotificationFont = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
)
private val NotificationPlum = Color(0xFF421735)
private val NotificationPeach = Color(0xFFFFCBBE)
private val NotificationMint = Color(0xFFC9EFE0)

/** Scoped palette: never changes the Home content under the sheet. */
@Composable
private fun NotificationSheetTheme(content: @Composable () -> Unit) {
    val base = MaterialTheme.colorScheme
    val dark = (base.surface.red * .299f + base.surface.green * .587f + base.surface.blue * .114f) < .5f
    val colors = base.copy(
        surface = if (dark) Color(0xFF171217) else Color(0xFFFFF5F0),
        background = if (dark) Color.Black else Color(0xFFFFF5F0),
        onSurface = if (dark) Color(0xFFFFF0F6) else Color(0xFF39132E),
        onSurfaceVariant = if (dark) Color(0xFFCFBECB) else Color(0xFF75616F),
        primary = if (dark) Color(0xFFCDBBE9) else Color(0xFF542347),
        outlineVariant = if (dark) Color(0xFF51414F) else Color(0xFFE8DEE5),
    )
    val t = MaterialTheme.typography
    MaterialTheme(colorScheme = colors, typography = t.copy(
        bodyLarge = t.bodyLarge.copy(fontFamily = NotificationFont),
        bodyMedium = t.bodyMedium.copy(fontFamily = NotificationFont),
        bodySmall = t.bodySmall.copy(fontFamily = NotificationFont),
        labelLarge = t.labelLarge.copy(fontFamily = NotificationFont),
        labelMedium = t.labelMedium.copy(fontFamily = NotificationFont),
        labelSmall = t.labelSmall.copy(fontFamily = NotificationFont),
    ), content = content)
}

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

    NotificationSheetTheme {
    ClampedUpdatesContent {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        ClampedUpdatesContent {
        AnnouncementsSheetContent(
            items = items,
            isLoading = isLoading,
            onMarkAllAsRead = onMarkAllAsRead,
            onMarkAsRead = onMarkAsRead,
            onDismissAnnouncement = onDismissAnnouncement,
            onAnnouncementAction = onAnnouncementAction,
            onClose = onDismissRequest,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.CenterHorizontally)
                .widthIn(max = 600.dp)
                // A tall measured sheet gives Material 3 a partially-expanded anchor.
                .fillMaxHeight(0.90f),
        )
        }
    }
    }
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
    onClose: () -> Unit = {},
) {
    val ink = rememberUpdatesInk()
    val isDarkTheme = MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }
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

    val unreadCount = remember(items) { items.count(NotificationFeedItem::isUnread) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            color = NotificationPlum, shape = RoundedCornerShape(24.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.announcements_title), Modifier.weight(1f),
                        fontFamily = NotificationFont, fontSize = 26.sp, lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold, color = Color.White)
                    IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Outlined.Close, stringResource(R.string.common_close), tint = Color.White)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    if (unreadCount > 0) {
                        Surface(color = NotificationMint, shape = CircleShape) {
                            Text(stringResource(R.string.announcements_new_count, unreadCount),
                                Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                color = NotificationPlum, fontFamily = NotificationFont,
                                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    } else {
                        Icon(Icons.Outlined.Notifications, null, tint = NotificationPeach, modifier = Modifier.size(32.dp))
                    }
                    Button(onClick = onMarkAllAsRead, enabled = unreadCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = NotificationPeach, contentColor = NotificationPlum,
                            disabledContainerColor = NotificationPeach.copy(alpha = .3f), disabledContentColor = Color.White.copy(alpha = .6f)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        shape = CircleShape, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Outlined.DoneAll, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.announcements_read_all), fontFamily = NotificationFont,
                            fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Modern Flat Filter Chips with live item counts (horizontally scrollable & unclipped)
        UpdatesFilterChips(
            items = items,
            selected = selectedFilter,
            onSelect = { selectedFilter = it },
            ink = ink,
            accent = accent,
            isDarkTheme = isDarkTheme,
            modifier = Modifier.fillMaxWidth(),
        )

        UpdatesHairline(ink.hairline)

        when {
            isLoading -> LoadingState(ink = ink, modifier = Modifier.weight(1f))
            items.isEmpty() -> EmptyAnnouncementsState(
                message = stringResource(R.string.announcements_all_caught_up),
                subtext = stringResource(R.string.announcements_none_right_now),
                ink = ink,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            filteredItems.isEmpty() -> EmptyAnnouncementsState(
                message = emptyMessageFor(selectedFilter),
                subtext = emptySubtextFor(selectedFilter),
                ink = ink,
                accent = accent,
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDarkTheme) Color(0xFF241D24) else Color(0xFFFFFDFC)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(
                    items = filteredItems,
                    key = { _, item -> item.id },
                ) { index, item ->
                    if (index > 0) UpdatesHairline(ink.hairline, Modifier.padding(horizontal = 4.dp))
                    AnnouncementRow(
                        item = item,
                        ink = ink,
                        accent = accent,
                        isDarkTheme = isDarkTheme,
                        isAudioPlaying = playingAudioItemId.value == item.id,
                        onMarkAsRead = { onMarkAsRead(item.id) },
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
private fun emptyMessageFor(filter: UpdatesFilter): String =
    when (filter) {
        UpdatesFilter.ALL -> stringResource(R.string.announcements_all_caught_up)
        UpdatesFilter.ANNOUNCEMENTS -> stringResource(R.string.announcements_no_announcements)
        UpdatesFilter.UPDATES -> stringResource(R.string.announcements_no_updates)
    }

@Composable
private fun emptySubtextFor(filter: UpdatesFilter): String =
    when (filter) {
        UpdatesFilter.ALL -> stringResource(R.string.announcements_check_back)
        UpdatesFilter.ANNOUNCEMENTS -> stringResource(R.string.announcements_general_here)
        UpdatesFilter.UPDATES -> stringResource(R.string.announcements_updates_here)
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
    items: List<NotificationFeedItem>,
    selected: UpdatesFilter,
    onSelect: (UpdatesFilter) -> Unit,
    ink: UpdatesInk,
    accent: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    val allCount = items.size
    val announcementCount = remember(items) {
        items.count { it.type == AnnouncementType.GENERAL || it.type == AnnouncementType.MAINTENANCE }
    }
    val updateCount = remember(items) {
        items.count { it.type == AnnouncementType.APP_UPDATE }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(UpdatesFilter.entries.toTypedArray()) { option ->
            val isSelected = option == selected
            val label = when (option) {
                UpdatesFilter.ALL -> stringResource(R.string.announcements_filter_all, allCount)
                UpdatesFilter.ANNOUNCEMENTS -> stringResource(R.string.announcements_filter_announcements, announcementCount)
                UpdatesFilter.UPDATES -> stringResource(R.string.announcements_filter_updates, updateCount)
            }
            Column(
                modifier = Modifier.width(androidx.compose.foundation.layout.IntrinsicSize.Max).clickable(role = Role.Tab) { onSelect(option) }
                    .semantics { this.selected = isSelected }.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSelected) accent else ink.secondaryText,
                        fontFamily = NotificationFont, fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1, softWrap = false)
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(if (isSelected) accent else Color.Transparent, CircleShape))
            }
        }
    }
}

@Composable
private fun AnnouncementRow(
    item: NotificationFeedItem,
    ink: UpdatesInk,
    accent: Color,
    isDarkTheme: Boolean,
    isAudioPlaying: Boolean,
    onMarkAsRead: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
    onToggleAudio: (String) -> Unit,
) {
    val context = LocalContext.current
    val isUpdate = item.type == AnnouncementType.APP_UPDATE
    val titleColor = if (item.isUnread) ink.primaryText else ink.secondaryText
    val bodyColor = if (item.isUnread) ink.primaryText.copy(alpha = 0.85f) else ink.mutedText
    val links = remember(item.body, item.deepLink) {
        parseAnnouncementLinks(item.body, item.deepLink)
    }

    val cardBg = Color.Transparent

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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = ::onRowClick,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = null,
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.padding(top = 4.dp).size(42.dp).clip(CircleShape)
                .background(if (isDarkTheme) Color(0xFF684047) else NotificationPeach.copy(alpha = .55f)),
                contentAlignment = Alignment.Center) {
                Icon(when (item.type) {
                    AnnouncementType.MAINTENANCE -> Icons.Outlined.WarningAmber
                    AnnouncementType.APP_UPDATE -> Icons.Outlined.NewReleases
                    else -> Icons.Outlined.Campaign
                }, null, tint = if (isDarkTheme) NotificationPeach else NotificationPlum, modifier = Modifier.size(24.dp))
            }
        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Category Badge & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    AnnouncementTypePill(
                        type = item.type,
                        isDarkTheme = isDarkTheme,
                    )
                    if (item.isUnread) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent),
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.announcements_hide_item, item.title),
                        tint = ink.mutedText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Notification Title
            Text(
                text = item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                lineHeight = 20.sp,
            )

            // Notification Body Content
            if (links.displayBody.isNotBlank()) {
                Text(
                    text = links.displayBody,
                    fontSize = 13.5.sp,
                    color = bodyColor,
                    lineHeight = 19.sp,
                )
            }

            // Play Store CTA Button for App Updates
            if (isUpdate) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.12f))
                        .clickable { onAction() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = stringResource(R.string.announcements_open_play_store),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }

            // Youtube Video Thumbnail
            links.youtubeVideoId?.let { videoId ->
                YoutubeThumb(
                    videoId = videoId,
                    ink = ink,
                    onClick = {
                        onMarkAsRead()
                        openUrl(links.youtubeUrl ?: YoutubeUrls.watchUrl(videoId))
                    },
                )
            }

            // Web Link Chip
            if (links.youtubeVideoId == null && !links.webUrl.isNullOrBlank()) {
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

            // Audio Player Row
            item.audioUrl?.takeIf { it.isNotBlank() }?.let { audioUrl ->
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

            // Footer Timestamp
            if (item.createdAt.isNotBlank()) {
                Text(
                    text = formatPostDate(item.createdAt),
                    fontSize = 11.5.sp,
                    color = ink.mutedText,
                )
            }
        }
        }
    }
}

@Composable
private fun AnnouncementTypePill(
    type: AnnouncementType,
    isDarkTheme: Boolean,
) {
    val (label, icon, color) = when (type) {
        AnnouncementType.APP_UPDATE -> Triple(
            stringResource(R.string.announcements_type_app_update),
            Icons.Outlined.NewReleases,
            if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7),
        )
        AnnouncementType.MAINTENANCE -> Triple(
            stringResource(R.string.announcements_type_maintenance),
            Icons.Outlined.WarningAmber,
            if (isDarkTheme) Color(0xFFF87171) else Color(0xFFDC2626),
        )
        AnnouncementType.GENERAL -> Triple(
            stringResource(R.string.announcements_type_announcement),
            Icons.Outlined.Campaign,
            if (isDarkTheme) Color(0xFFC084FC) else Color(0xFF542347),
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = color,
        )
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
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, ink.hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumbUrl.isNotBlank()) {
            AsyncImage(
                model = thumbUrl,
                contentDescription = stringResource(R.string.announcements_youtube_video),
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
            contentDescription = stringResource(R.string.announcements_open_youtube),
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
    val openLink = stringResource(R.string.announcements_open_link)
    val host = remember(url, openLink) {
        runCatching { Uri.parse(url).host }.getOrNull().orEmpty().ifBlank { openLink }
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, ink.hairline, RoundedCornerShape(8.dp))
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
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isPlaying) accent else ink.hairline, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(
                if (isPlaying) R.string.announcements_pause_audio else R.string.announcements_play_audio
            ),
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(
                if (isPlaying) R.string.announcements_playing_audio else R.string.announcements_play_audio
            ),
            style = MaterialTheme.typography.labelLarge,
            color = ink.primaryText,
        )
    }
}

@Composable
private fun LoadingState(
    ink: UpdatesInk,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) {
            com.safarparmar.app.ui.components.AnnouncementRowSkeleton()
        }
    }
}

@Composable
private fun EmptyAnnouncementsState(
    message: String,
    subtext: String,
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
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = ink.primaryText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtext,
            fontSize = 12.5.sp,
            color = ink.mutedText,
            textAlign = TextAlign.Center,
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

/** Keep the notification header, tabs and cards within the app's font-scale bounds. */
@Composable
private fun ClampedUpdatesContent(content: @Composable () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(
            density.density, density.fontScale.coerceIn(0.85f, 1.05f)
        )
    ) { content() }
}
