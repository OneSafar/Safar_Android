package com.safarparmar.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class WeeklyLeaderboardPeriodDto(
    @SerializedName("start") val start: String? = null,
    @SerializedName("end") val end: String? = null,
    @SerializedName("timezone") val timezone: String? = "Asia/Kolkata",
)

data class WeeklyLeaderboardEntryDto(
    @SerializedName("rank") val rank: Int = 0,
    @SerializedName("userId") val userId: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("avatar") val avatar: String? = null,
    @SerializedName("totalFocusMinutes") val totalFocusMinutes: Int = 0,
    @SerializedName("score") val score: Double = 0.0,
    @SerializedName("plannedFocusMinutes") val plannedFocusMinutes: Int? = null,
    @SerializedName("fullyCompletedFocusSessions") val fullyCompletedFocusSessions: Int? = null,
)

data class WeeklyLeaderboardResponseDto(
    @SerializedName("period") val period: WeeklyLeaderboardPeriodDto? = null,
    @SerializedName("podiumPeriod") val podiumPeriod: WeeklyLeaderboardPeriodDto? = null,
    @SerializedName("page") val page: Int = 1,
    @SerializedName("pageSize") val pageSize: Int = 20,
    @SerializedName("totalEntries") val totalEntries: Int = 0,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("podium") val podium: List<WeeklyLeaderboardEntryDto> = emptyList(),
    @SerializedName("entries") val entries: List<WeeklyLeaderboardEntryDto> = emptyList(),
    @SerializedName("currentUserRank") val currentUserRank: Int? = null,
    @SerializedName("currentUserEntry") val currentUserEntry: WeeklyLeaderboardEntryDto? = null,
)
