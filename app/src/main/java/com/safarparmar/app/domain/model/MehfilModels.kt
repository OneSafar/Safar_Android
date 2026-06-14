package com.safarparmar.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class MehfilPost(
    val id: String = "",
    val content: String = "",
    val space: String = "",
    val authorName: String = "",
    val authorAvatar: String? = null,
    val userId: String = "",
    val createdAt: String = "",
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    val userLiked: Boolean = false
)

@Immutable
data class Sandesh(
    val id: String = "",
    val content: String = "",
    val createdAt: String = "",
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    val linkMeta: LinkMeta? = null,
    val imageUrl: String = ""
)

@Immutable
data class LinkMeta(val title: String = "", val description: String = "", val image: String = "", val url: String = "")

@Immutable
data class Comment(val id: String = "", val content: String = "", val authorName: String = "", val createdAt: String = "")

@Immutable
data class ActivityItem(
    val type: String = "",
    val createdAt: String = "",
    val thoughtId: String = "",
    val comment: String? = null,
    val thoughtContent: String = "",
    val thoughtAuthor: String = "",
    val thoughtCategory: String = ""
)
