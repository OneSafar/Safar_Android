package com.safarparmar.app.ui.home

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
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DoneAll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safarparmar.app.domain.model.AnnouncementType
import com.safarparmar.app.domain.model.NotificationFeedItem
import com.safarparmar.app.domain.model.NotificationFeedSource
import com.safarparmar.app.ui.mehfil.formatPostDate
import com.safarparmar.app.ui.theme.SafarTheme

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
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Announcements & Updates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
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
                        onClick = { onMarkAsRead(item.id) },
                        onDismiss = { onDismissAnnouncement(item.id) },
                        onAction = { onAnnouncementAction(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnouncementRow(
    item: NotificationFeedItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onAction: () -> Unit,
) {
    val contentAlpha = if (item.isUnread) 1f else 0.62f
    val containerColor = if (item.isUnread) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
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

                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = item.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                        maxLines = if (item.type == AnnouncementType.APP_UPDATE) 2 else 3,
                        overflow = TextOverflow.Ellipsis,
                    )

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
                        contentDescription = "Dismiss ${item.title}",
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
        id = "maintenance",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.MAINTENANCE,
        title = "Scheduled System Maintenance",
        body = "Scheduled System Maintenance tonight at 12:00 AM UTC.",
        createdAt = "2026-07-09T18:00:00Z",
        isUnread = true,
    ),
    NotificationFeedItem(
        id = "general",
        source = NotificationFeedSource.CUSTOM,
        type = AnnouncementType.GENERAL,
        title = "A fresh new dashboard",
        body = "Welcome to our newly redesigned dashboard. Everything you use is still close at hand.",
        createdAt = "2026-07-08T10:30:00Z",
        isUnread = false,
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
