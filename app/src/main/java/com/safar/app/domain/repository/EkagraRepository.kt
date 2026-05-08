package com.safar.app.domain.repository

import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.domain.model.EkagraAnalyticsStats
import com.safar.app.util.Resource

interface EkagraRepository {
    suspend fun getStats(): Resource<FocusStatsResponse>
    suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats>
    suspend fun saveSession(
        mode: String,
        startedAt: String,
        endedAt: String?,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        goalId: String? = null,
        goalTitle: String? = null,
        taskTitle: String? = null,
        markGoalComplete: Boolean = false,
    ): Resource<EkagraSession>
    suspend fun deleteSession(sessionId: String): Resource<Unit>
}
