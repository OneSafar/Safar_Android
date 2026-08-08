package com.safarparmar.app.feature.kavachanalytics.data.remote

import com.google.gson.annotations.SerializedName

// ── Upload ───────────────────────────────────────────────────────────────────

data class DailyAggregateUploadDto(
    @SerializedName("localDate") val localDate: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("appLabel") val appLabel: String?,
    @SerializedName("category") val category: String,
    @SerializedName("allDaySeconds") val allDaySeconds: Int,
    @SerializedName("kavachSeconds") val kavachSeconds: Int,
    @SerializedName("blockedAttempts") val blockedAttempts: Int,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class DailyAggregateBatchRequest(
    @SerializedName("timezone") val timezone: String,
    @SerializedName("aggregates") val aggregates: List<DailyAggregateUploadDto>,
)

data class YoutubeDailyAggregateUploadDto(
    @SerializedName("localDate") val localDate: String,
    @SerializedName("productiveSeconds") val productiveSeconds: Int,
    @SerializedName("distractingSeconds") val distractingSeconds: Int,
    @SerializedName("shortsSeconds") val shortsSeconds: Int,
    @SerializedName("unidentifiedSeconds") val unidentifiedSeconds: Int,
    @SerializedName("protectedProductiveSeconds") val protectedProductiveSeconds: Int,
    @SerializedName("protectedDistractingSeconds") val protectedDistractingSeconds: Int,
    @SerializedName("protectedShortsSeconds") val protectedShortsSeconds: Int,
    @SerializedName("protectedUnidentifiedSeconds") val protectedUnidentifiedSeconds: Int,
    @SerializedName("coverage") val coverage: String,
    @SerializedName("updatedAt") val updatedAt: String,
)

data class YoutubeDailyAggregateBatchRequest(
    @SerializedName("timezone") val timezone: String,
    @SerializedName("aggregates") val aggregates: List<YoutubeDailyAggregateUploadDto>,
)

data class SessionUploadDto(
    @SerializedName("clientSessionId") val clientSessionId: String,
    @SerializedName("startedAt") val startedAt: String,
    @SerializedName("endedAt") val endedAt: String?,
    @SerializedName("localDate") val localDate: String,
    @SerializedName("plannedSeconds") val plannedSeconds: Int,
    @SerializedName("actualSeconds") val actualSeconds: Int,
    @SerializedName("mode") val mode: String,
    @SerializedName("outcome") val outcome: String?,
    @SerializedName("blockedAttempts") val blockedAttempts: Int,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int,
    @SerializedName("quickUnlockSeconds") val quickUnlockSeconds: Int,
    @SerializedName("productiveSeconds") val productiveSeconds: Int,
    @SerializedName("distractingSeconds") val distractingSeconds: Int,
    @SerializedName("neutralSeconds") val neutralSeconds: Int,
    @SerializedName("unclassifiedSeconds") val unclassifiedSeconds: Int,
    @SerializedName("permissionLost") val permissionLost: Boolean,
    @SerializedName("dataGap") val dataGap: Boolean,
)

data class SessionBatchRequest(
    @SerializedName("timezone") val timezone: String,
    @SerializedName("sessions") val sessions: List<SessionUploadDto>,
)

data class AppClassificationDto(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("category") val category: String,
    @SerializedName("appLabel") val appLabel: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
)

data class AppClassificationBatchRequest(
    @SerializedName("classifications") val classifications: List<AppClassificationDto>,
)

data class AppClassificationListResponse(
    @SerializedName("classifications") val classifications: List<AppClassificationDto>? = null,
)

data class BatchAckResponse(
    @SerializedName("accepted") val accepted: Int? = null,
    @SerializedName("message") val message: String? = null,
)

// ── Read ─────────────────────────────────────────────────────────────────────

data class CategoryTotalsDto(
    @SerializedName("productiveSeconds") val productiveSeconds: Int? = null,
    @SerializedName("distractingSeconds") val distractingSeconds: Int? = null,
    @SerializedName("neutralSeconds") val neutralSeconds: Int? = null,
    @SerializedName("unclassifiedSeconds") val unclassifiedSeconds: Int? = null,
)

data class TrendPointDto(
    @SerializedName("localDate") val localDate: String? = null,
    @SerializedName("allDay") val allDay: CategoryTotalsDto? = null,
    @SerializedName("duringKavach") val duringKavach: CategoryTotalsDto? = null,
    @SerializedName("blockedAttempts") val blockedAttempts: Int? = null,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int? = null,
    @SerializedName("coverage") val coverage: String? = null,
)

data class AppBreakdownDto(
    @SerializedName("packageName") val packageName: String? = null,
    @SerializedName("appLabel") val appLabel: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("allDaySeconds") val allDaySeconds: Int? = null,
    @SerializedName("kavachSeconds") val kavachSeconds: Int? = null,
    @SerializedName("blockedAttempts") val blockedAttempts: Int? = null,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int? = null,
)

data class SessionHistoryDto(
    @SerializedName("clientSessionId") val clientSessionId: String? = null,
    @SerializedName("startedAt") val startedAt: String? = null,
    @SerializedName("endedAt") val endedAt: String? = null,
    @SerializedName("plannedSeconds") val plannedSeconds: Int? = null,
    @SerializedName("actualSeconds") val actualSeconds: Int? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("outcome") val outcome: String? = null,
    @SerializedName("blockedAttempts") val blockedAttempts: Int? = null,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int? = null,
    @SerializedName("quickUnlockSeconds") val quickUnlockSeconds: Int? = null,
    @SerializedName("productiveSeconds") val productiveSeconds: Int? = null,
    @SerializedName("distractingSeconds") val distractingSeconds: Int? = null,
    @SerializedName("neutralSeconds") val neutralSeconds: Int? = null,
    @SerializedName("unclassifiedSeconds") val unclassifiedSeconds: Int? = null,
    @SerializedName("permissionLost") val permissionLost: Boolean? = null,
    @SerializedName("dataGap") val dataGap: Boolean? = null,
)

data class KavachReportResponse(
    @SerializedName("rangeStart") val rangeStart: String? = null,
    @SerializedName("rangeEnd") val rangeEnd: String? = null,
    @SerializedName("allDay") val allDay: CategoryTotalsDto? = null,
    @SerializedName("duringKavach") val duringKavach: CategoryTotalsDto? = null,
    @SerializedName("trend") val trend: List<TrendPointDto>? = null,
    @SerializedName("apps") val apps: List<AppBreakdownDto>? = null,
    @SerializedName("sessions") val sessions: List<SessionHistoryDto>? = null,
    @SerializedName("blockedAttempts") val blockedAttempts: Int? = null,
    @SerializedName("quickUnlockCount") val quickUnlockCount: Int? = null,
    @SerializedName("quickUnlockSeconds") val quickUnlockSeconds: Int? = null,
    @SerializedName("completedSessions") val completedSessions: Int? = null,
    @SerializedName("endedEarlySessions") val endedEarlySessions: Int? = null,
    @SerializedName("interruptedSessions") val interruptedSessions: Int? = null,
    @SerializedName("coverage") val coverage: String? = null,
    @SerializedName("daysMissingCoverage") val daysMissingCoverage: List<String>? = null,
    @SerializedName("youtube") val youtube: YoutubeReportDto? = null,
)

data class YoutubeReportDto(
    @SerializedName("productiveSeconds") val productiveSeconds: Int? = null,
    @SerializedName("distractingSeconds") val distractingSeconds: Int? = null,
    @SerializedName("shortsSeconds") val shortsSeconds: Int? = null,
    @SerializedName("unidentifiedSeconds") val unidentifiedSeconds: Int? = null,
    @SerializedName("protectedProductiveSeconds") val protectedProductiveSeconds: Int? = null,
    @SerializedName("protectedDistractingSeconds") val protectedDistractingSeconds: Int? = null,
    @SerializedName("protectedShortsSeconds") val protectedShortsSeconds: Int? = null,
    @SerializedName("protectedUnidentifiedSeconds") val protectedUnidentifiedSeconds: Int? = null,
)
