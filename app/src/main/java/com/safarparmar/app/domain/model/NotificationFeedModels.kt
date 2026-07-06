package com.safarparmar.app.domain.model

import androidx.compose.runtime.Immutable

enum class NotificationFeedSource { CUSTOM }

@Immutable
data class NotificationFeedItem(
    val id: String,
    val source: NotificationFeedSource,
    val title: String,
    val body: String,
    val createdAt: String,
    val deepLink: String? = null,
    val isUnread: Boolean = false,
)
