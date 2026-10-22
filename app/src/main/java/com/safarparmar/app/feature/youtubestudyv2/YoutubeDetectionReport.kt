package com.safarparmar.app.feature.youtubestudyv2

/** In-memory capture; written and shared only when the user requests a report. */
@androidx.annotation.Keep
internal data class YoutubeDetectionReport(
    val formatVersion: Int = 1,
    val appVersion: String,
    val appVersionCode: Int,
    val capturedAtEpochMs: Long,
    val nodeLimitReached: Boolean,
    val snapshot: YoutubeV2Snapshot,
    val parsed: YoutubeV2Observation,
    val effective: YoutubeV2Observation,
)
