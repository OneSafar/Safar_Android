package com.safarparmar.app.domain.model

import androidx.compose.runtime.Immutable

enum class NotificationFeedSource { CUSTOM }

enum class AnnouncementType {
    APP_UPDATE,
    MAINTENANCE,
    GENERAL,
}

@Immutable
data class NotificationFeedItem(
    val id: String,
    val source: NotificationFeedSource,
    val title: String,
    val body: String,
    val createdAt: String,
    val deepLink: String? = null,
    val isUnread: Boolean = false,
    val type: AnnouncementType = AnnouncementType.GENERAL,
)
