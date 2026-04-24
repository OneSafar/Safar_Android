package com.safar.app.domain.repository

import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.util.Resource

interface EkagraRepository {
    suspend fun getStats(): Resource<FocusStatsResponse>
    suspend fun activateSession(title: String, totalSeconds: Int, sessionStartedAt: String): Resource<EkagraSession>
    suspend fun completeSession(sessionId: String, totalSeconds: Int, elapsedSeconds: Int, remainingSeconds: Int, sessionStartedAt: String?): Resource<EkagraSession>
    suspend fun getActiveSession(): Resource<EkagraSession?>
}
