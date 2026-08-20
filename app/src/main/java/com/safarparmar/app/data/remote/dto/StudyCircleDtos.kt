package com.safarparmar.app.data.remote.dto

data class StudyCircleSummaryDto(
    val id: String = "",
    val name: String = "",
    val role: String = "member",
    val visibility: String = "private",
    val memberCount: Int = 0,
    val focusingCount: Int = 0,
    val maxMembers: Int? = null,
    val createdAt: String = "",
)

data class StudyCircleMemberDto(
    val userId: String = "",
    val name: String = "Safar learner",
    val avatar: String? = null,
    val role: String = "member",
    val joinedAt: String = "",
    val isFocusing: Boolean = false,
    val isPremium: Boolean = false,
)

data class StudyCircleDetailDto(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val role: String = "member",
    val visibility: String = "private",
    val memberCount: Int = 0,
    val maxMembers: Int? = null,
    val focusingCount: Int = 0,
    val createdAt: String = "",
    val joinCode: String? = null,
    val members: List<StudyCircleMemberDto> = emptyList(),
)

data class PublicStudyCircleDto(
    val id: String = "",
    val name: String = "",
    val ownerName: String = "Safar learner",
    val ownerAvatar: String? = null,
    val memberCount: Int = 0,
    val focusingCount: Int = 0,
    val maxMembers: Int? = null,
    val joined: Boolean = false,
    val createdAt: String = "",
)

data class StudyCircleLeaderboardEntryDto(
    val rank: Int = 0,
    val userId: String = "",
    val name: String = "Safar learner",
    val avatar: String? = null,
    val totalFocusMinutes: Int = 0,
    val sessionCount: Int = 0,
    val nextRankGapMinutes: Int? = null,
    val isFocusing: Boolean = false,
)

data class StudyCirclePeriodDto(
    val start: String = "",
    val end: String = "",
    val timezone: String = "Asia/Kolkata",
)

data class StudyCirclesResponse(val circles: List<StudyCircleSummaryDto> = emptyList())
data class PublicStudyCirclesResponse(
    val circles: List<PublicStudyCircleDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 15,
    val hasMore: Boolean = false,
)
data class StudyCircleResponse(val circle: StudyCircleDetailDto = StudyCircleDetailDto())
data class CreatedStudyCircleResponse(val circle: StudyCircleSummaryDto = StudyCircleSummaryDto())
data class JoinStudyCircleResponse(val circleId: String = "", val alreadyMember: Boolean = false)
data class StudyCircleLeaderboardResponse(
    val circleId: String = "",
    val period: StudyCirclePeriodDto = StudyCirclePeriodDto(),
    val entries: List<StudyCircleLeaderboardEntryDto> = emptyList(),
)
data class StudyCircleVisibilityResponse(val visibility: String = "private")
data class StudyCircleActionResponse(
    val left: Boolean? = null,
    val removed: Boolean? = null,
    val deleted: Boolean? = null,
)

data class StudyCircleRenameResponse(
    val name: String = "",
    val circleId: String = "",
)

data class StudyCircleLiveSummaryDto(
    val totalFocusing: Int = 0,
    val activeCirclesCount: Int = 0,
)

data class CreateStudyCircleRequest(val name: String, val visibility: String)
data class UpdateStudyCircleNameRequest(val name: String)
data class JoinStudyCircleRequest(val code: String)
data class SetStudyCircleVisibilityRequest(val visibility: String)
data class FocusPresenceRequest(val active: Boolean)
