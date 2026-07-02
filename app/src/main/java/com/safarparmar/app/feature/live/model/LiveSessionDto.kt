package com.safarparmar.app.feature.live.model

import com.google.gson.annotations.SerializedName

data class LiveSessionResourceDto(
    @SerializedName("label")
    val label: String? = null,
    @SerializedName("url")
    val url: String? = null,
)

data class LiveSessionDto(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("description")
    val description: String? = null,
    @SerializedName("courseId")
    val courseId: String? = null,
    @SerializedName("teacherId")
    val teacherId: String? = null,
    @SerializedName("scheduledStartAt")
    val scheduledStartAt: String? = null,
    @SerializedName("scheduledEndAt")
    val scheduledEndAt: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("youtubeVideoId")
    val youtubeVideoId: String? = null,
    @SerializedName("youtubeWatchUrl")
    val youtubeWatchUrl: String? = null,
    @SerializedName("youtubeEmbedUrl")
    val youtubeEmbedUrl: String? = null,
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,
    @SerializedName("isChatEnabled")
    val isChatEnabled: Boolean? = null,
    @SerializedName("isRecordingAvailable")
    val isRecordingAvailable: Boolean? = null,
    @SerializedName("recordingVideoId")
    val recordingVideoId: String? = null,
    @SerializedName("resources")
    val resources: List<LiveSessionResourceDto?>? = null,
    @SerializedName("canManage")
    val canManage: Boolean? = null,
    @SerializedName("createdBy")
    val createdBy: String? = null,
    @SerializedName("createdAt")
    val createdAt: String? = null,
    @SerializedName("updatedAt")
    val updatedAt: String? = null,
)

data class LiveSessionsResponseDto(
    @SerializedName("liveSessions")
    val liveSessions: List<LiveSessionDto>? = null,
)

data class LiveSessionResponseDto(
    @SerializedName("liveSession")
    val liveSession: LiveSessionDto? = null,
)

data class LiveSessionResource(
    val label: String,
    val url: String,
)

data class LiveSession(
    val id: String,
    val title: String,
    val description: String?,
    val courseId: String,
    val teacherId: String?,
    val scheduledStartAt: String?,
    val scheduledEndAt: String?,
    val status: String,
    val youtubeVideoId: String?,
    val youtubeWatchUrl: String?,
    val youtubeEmbedUrl: String?,
    val thumbnailUrl: String?,
    val isChatEnabled: Boolean,
    val isRecordingAvailable: Boolean,
    val recordingVideoId: String?,
    val resources: List<LiveSessionResource>,
    val canManage: Boolean,
)

fun LiveSessionDto.toDomain(): LiveSession = LiveSession(
    id = id.orEmpty(),
    title = title.orEmpty(),
    description = description,
    courseId = courseId.orEmpty(),
    teacherId = teacherId,
    scheduledStartAt = scheduledStartAt,
    scheduledEndAt = scheduledEndAt,
    status = status.orEmpty(),
    youtubeVideoId = youtubeVideoId,
    youtubeWatchUrl = youtubeWatchUrl,
    youtubeEmbedUrl = youtubeEmbedUrl,
    thumbnailUrl = thumbnailUrl,
    isChatEnabled = isChatEnabled ?: true,
    isRecordingAvailable = isRecordingAvailable ?: false,
    recordingVideoId = recordingVideoId,
    resources = resources.orEmpty().mapNotNull { resource ->
        val label = resource?.label?.trim().orEmpty()
        val url = resource?.url?.trim().orEmpty()
        if (label.isBlank() || url.isBlank()) null else LiveSessionResource(label, url)
    },
    canManage = canManage ?: false,
)
