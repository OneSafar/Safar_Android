package com.safar.app.data.repository

import com.safar.app.data.remote.api.FocusApi
import com.safar.app.data.remote.dto.ActivateSessionRequest
import com.safar.app.data.remote.dto.CompleteSessionRequest
import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.domain.repository.EkagraRepository
import com.safar.app.util.Resource
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EkagraRepositoryImpl @Inject constructor(
    private val focusApi: FocusApi,
) : EkagraRepository {

    override suspend fun getStats(): Resource<FocusStatsResponse> {
        val res = focusApi.getStats()
        return if (res.isSuccessful) Resource.Success(res.body()!!)
        else Resource.Error("Error ${res.code()}")
    }

    override suspend fun activateSession(
        title: String,
        totalSeconds: Int,
        sessionStartedAt: String,
    ): Resource<EkagraSession> {
        val res = focusApi.activateSession(
            ActivateSessionRequest(
                sessionTitle      = title.ifBlank { "Focus Session" },
                totalSeconds      = totalSeconds,
                remainingSeconds  = totalSeconds,
                sessionStartedAt  = sessionStartedAt,
            )
        )
        return if (res.isSuccessful) Resource.Success(res.body()!!.session)
        else Resource.Error("Activate failed: ${res.code()}")
    }

    override suspend fun completeSession(
        sessionId: String,
        totalSeconds: Int,
        elapsedSeconds: Int,
        remainingSeconds: Int,
        sessionStartedAt: String?,
    ): Resource<EkagraSession> {
        val res = focusApi.completeSession(
            sessionId = sessionId,
            request   = CompleteSessionRequest(
                totalSeconds      = totalSeconds,
                elapsedSeconds    = elapsedSeconds,
                remainingSeconds  = remainingSeconds,
                sessionStartedAt  = sessionStartedAt,
            )
        )
        return if (res.isSuccessful) Resource.Success(res.body()!!.session)
        else Resource.Error("Complete failed: ${res.code()}")
    }

    override suspend fun getActiveSession(): Resource<EkagraSession?> {
        val res = focusApi.getActiveSession()
        return if (res.isSuccessful) Resource.Success(res.body()?.session)
        else Resource.Error("Error ${res.code()}")
    }
}
