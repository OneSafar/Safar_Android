package com.safarparmar.app.feature.live.model

data class LiveSessionResourceDto(
    val label: String? = null,
    val url: String? = null,
)

data class LiveSessionDto(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val courseId: String? = null,
    val teacherId: String? = null,
    val scheduledStartAt: String? = null,
    val scheduledEndAt: String? = null,
    val status: String? = null,
    val youtubeVideoId: String? = null,
    val youtubeWatchUrl: String? = null,
    val youtubeEmbedUrl: String? = null,
    val thumbnailUrl: String? = null,
    val isChatEnabled: Boolean? = null,
    val isRecordingAvailable: Boolean? = null,
    val recordingVideoId: String? = null,
    val resources: List<LiveSessionResourceDto>? = null,
    val canManage: Boolean? = null,
    val createdBy: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

data class LiveSessionsResponseDto(
    val liveSessions: List<LiveSessionDto>? = null,
)

data class LiveSessionResponseDto(
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
        val label = resource.label?.trim().orEmpty()
        val url = resource.url?.trim().orEmpty()
        if (label.isBlank() || url.isBlank()) null else LiveSessionResource(label, url)
    },
    canManage = canManage ?: false,
)
