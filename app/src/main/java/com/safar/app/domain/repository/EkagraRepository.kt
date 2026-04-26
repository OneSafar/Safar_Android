package com.safar.app.domain.repository

import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.domain.model.EkagraAnalyticsStats
import com.safar.app.util.Resource

interface EkagraRepository {
    suspend fun getStats(): Resource<FocusStatsResponse>
    suspend fun getOpenSessions(): Resource<List<EkagraSession>>
    suspend fun getActiveSession(): Resource<EkagraSession?>
    suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats>
    suspend fun activateSession(
        title: String,
        totalSeconds: Int,
        sessionStartedAt: String,
        goalId: String? = null,
        goalTitle: String? = null,
        mode: String = "Timer",
        remainingSeconds: Int = totalSeconds,
    ): Resource<EkagraSession>
    suspend fun updateSession(
        sessionId: String,
        status: String? = null,
        mode: String? = null,
        totalSeconds: Int? = null,
        remainingSeconds: Int? = null,
        isRunning: Boolean? = null,
        sessionStartedAt: String? = null,
        goalTitle: String? = null,
        source: String? = null,
        importedFromGoal: Boolean? = null,
    ): Resource<EkagraSession>
    suspend fun completeSession(sessionId: String, totalSeconds: Int, elapsedSeconds: Int, remainingSeconds: Int, sessionStartedAt: String?, mode: String = "Timer"): Resource<EkagraSession>
    suspend fun discardSession(sessionId: String): Resource<EkagraSession>
    suspend fun deleteSession(sessionId: String): Resource<Unit>
}
