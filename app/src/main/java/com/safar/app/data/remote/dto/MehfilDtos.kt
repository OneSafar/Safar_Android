package com.safar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MehfilPostDto(
    @SerializedName("_id")          val mongoId: String? = null,
    val id: String? = null,
    val content: String? = null,
    val space: String? = null,
    val category: String? = null,
    val isAnonymous: Boolean? = null,
    val author: MehfilAuthorDto? = null,
    // Flat fields returned by some API variants
    val authorName: String? = null,
    val authorAvatar: String? = null,
    val userId: String? = null,
    @SerializedName("created_at")   val createdAt: String? = null,
    // API may use relatableCount/commentsCount OR reactionCount/commentCount
    val relatableCount: Int? = null,
    val reactionCount: Int? = null,
    val commentsCount: Int? = null,
    val commentCount: Int? = null,
    val hasReacted: Boolean? = null,
    val userLiked: Boolean? = false
)

data class MehfilAuthorDto(val id: String? = null, val name: String? = null, val avatar: String? = null)

data class SandeshDto(
    @SerializedName("_id")          val mongoId: String? = null,
    val id: String? = null,
    val content: String? = null,
    val importance: String? = null,
    @SerializedName("link_meta")    val linkMeta: LinkMetaDto? = null,
    @SerializedName("image_url")    val imageUrl: String? = null,
    @SerializedName("audio_url")    val audioUrl: String? = null,
    @SerializedName("author_id")    val authorId: String? = null,
    @SerializedName("created_at")   val createdAt: String? = null,
    val reactionCount: Int? = 0,
    val commentCount: Int? = 0,
    val userLiked: Boolean? = false
)

data class LinkMetaDto(val title: String? = null, val description: String? = null, val image: String? = null, val url: String? = null)

data class SandeshResponse(
    val sandesh: SandeshDto? = null,
    val sandeshes: List<SandeshDto>? = null,
    val isAdmin: Boolean? = false
)

data class MehfilFeedResponse(
    val posts: List<MehfilPostDto>? = null,
    val page: Int? = 1,
    val totalPages: Int? = 1
)

data class CreatePostRequest(val content: String, val space: String = "thoughts")

data class CommentsResponse(
    val comments: List<CommentDto>? = null,
    val page: Int? = 1,
    val hasMore: Boolean? = false
)

data class CommentDto(
    val id: String? = null,
    val content: String? = null,
    val authorName: String? = null,
    val createdAt: String? = null
)

data class CommentRequest(val thoughtId: String? = null, val content: String)

data class SaveRequest(val thoughtId: String)

data class ActivityResponse(val items: List<ActivityItemDto>? = null)

data class ActivityItemDto(
    val type: String? = null,
    val createdAt: String? = null,
    val thoughtId: String? = null,
    val comment: String? = null,
    val thought: ActivityThoughtDto? = null
)

data class ActivityThoughtDto(
    val id: String? = null,
    val authorName: String? = null,
    val category: String? = null,
    val content: String? = null
)

data class SavedPostsResponse(
    val posts: List<MehfilPostDto>? = null,
    val page: Int? = 1,
    val hasMore: Boolean? = false
)
