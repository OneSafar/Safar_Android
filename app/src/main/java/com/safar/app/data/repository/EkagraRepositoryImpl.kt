package com.safar.app.data.repository

import com.safar.app.data.remote.api.FocusApi
import com.safar.app.data.remote.dto.ActivateSessionRequest
import com.safar.app.data.remote.dto.CompleteSessionRequest
import com.safar.app.data.remote.dto.EkagraAnalyticsFocusSessionDto
import com.safar.app.data.remote.dto.EkagraAnalyticsRecentSessionDto
import com.safar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safar.app.data.remote.dto.EkagraSession
import com.safar.app.data.remote.dto.FocusStatsResponse
import com.safar.app.data.remote.dto.UpdateEkagraSessionRequest
import com.safar.app.domain.model.EkagraAnalyticsFocusSession
import com.safar.app.domain.model.EkagraAnalyticsRecentSession
import com.safar.app.domain.model.EkagraAnalyticsStats
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

    override suspend fun getOpenSessions(): Resource<List<EkagraSession>> {
        val res = focusApi.getSessions()
        return if (res.isSuccessful) Resource.Success(res.body()?.sessions.orEmpty().map { it.normalized() })
        else Resource.Error("Sessions failed: ${res.code()}")
    }

    override suspend fun getActiveSession(): Resource<EkagraSession?> {
        val res = focusApi.getActiveSession()
        return if (res.isSuccessful) Resource.Success(res.body()?.session?.normalized())
        else Resource.Error("Error ${res.code()}")
    }

    override suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats> {
        val res = focusApi.getEkagraAnalytics()
        return if (res.isSuccessful) Resource.Success((res.body() ?: EkagraAnalyticsStatsDto()).toDomain())
        else Resource.Error("Analytics failed: ${res.code()}")
    }

    override suspend fun activateSession(
        title: String,
        totalSeconds: Int,
        sessionStartedAt: String,
        goalId: String?,
        goalTitle: String?,
        mode: String,
        remainingSeconds: Int,
    ): Resource<EkagraSession> {
        val res = focusApi.activateSession(
            ActivateSessionRequest(
                sessionType       = if (goalId.isNullOrBlank()) "named" else "goal",
                goalId            = goalId,
                goalTitle         = goalTitle,
                sessionTitle      = title.ifBlank { goalTitle ?: "Focus Session" },
                source            = if (goalId.isNullOrBlank()) "manual" else "goal_continue",
                totalSeconds      = totalSeconds,
                remainingSeconds  = remainingSeconds,
                mode              = mode,
                sessionStartedAt  = sessionStartedAt,
            )
        )
        return if (res.isSuccessful) Resource.Success(res.body()!!.session.normalized())
        else Resource.Error("Activate failed: ${res.code()}")
    }

    override suspend fun updateSession(
        sessionId: String,
        status: String?,
        mode: String?,
        totalSeconds: Int?,
        remainingSeconds: Int?,
        isRunning: Boolean?,
        sessionStartedAt: String?,
        goalTitle: String?,
        source: String?,
        importedFromGoal: Boolean?,
    ): Resource<EkagraSession> {
        val res = focusApi.updateSession(
            sessionId,
            UpdateEkagraSessionRequest(
                status = status,
                mode = mode,
                totalSeconds = totalSeconds,
                remainingSeconds = remainingSeconds,
                isRunning = isRunning,
                sessionStartedAt = sessionStartedAt,
                goalTitle = goalTitle,
                source = source,
                importedFromGoal = importedFromGoal,
            ),
        )
        return if (res.isSuccessful) Resource.Success(res.body()!!.session.normalized())
        else Resource.Error("Update failed: ${res.code()}")
    }

    override suspend fun completeSession(
        sessionId: String,
        totalSeconds: Int,
        elapsedSeconds: Int,
        remainingSeconds: Int,
        sessionStartedAt: String?,
        mode: String,
    ): Resource<EkagraSession> {
        val res = focusApi.completeSession(
            sessionId = sessionId,
            request   = CompleteSessionRequest(
                mode              = mode,
                totalSeconds      = totalSeconds,
                elapsedSeconds    = elapsedSeconds,
                remainingSeconds  = remainingSeconds,
                sessionStartedAt  = sessionStartedAt,
            )
        )
        return if (res.isSuccessful) Resource.Success(res.body()!!.session.normalized())
        else Resource.Error("Complete failed: ${res.code()}")
    }

    override suspend fun discardSession(sessionId: String): Resource<EkagraSession> {
        val res = focusApi.discardSession(sessionId)
        return if (res.isSuccessful) Resource.Success(res.body()!!.session.normalized())
        else Resource.Error("Discard failed: ${res.code()}")
    }

    override suspend fun deleteSession(sessionId: String): Resource<Unit> {
        val res = focusApi.deleteSession(sessionId)
        return if (res.isSuccessful) Resource.Success(Unit)
        else Resource.Error("Delete failed: ${res.code()}")
    }

    private fun EkagraSession.normalized() = copy(
        userId = userId.ifBlank { userIdSnake ?: "" },
        goalId = goalId ?: goalIdSnake,
        goalTitle = goalTitle ?: goalTitleSnake,
        sessionType = sessionTypeSnake ?: sessionType,
        sessionTitle = sessionTitle ?: sessionTitleSnake,
        totalSeconds = totalSecondsSnake ?: totalSeconds,
        remainingSeconds = remainingSecondsSnake ?: remainingSeconds,
        isRunning = isRunningSnake ?: isRunning,
        importedFromGoal = importedFromGoalSnake ?: importedFromGoal,
        pauseCount = pauseCountSnake ?: pauseCount,
        sessionStartedAt = sessionStartedAt ?: sessionStartedAtSnake,
        createdAt = createdAt.ifBlank { createdAtSnake ?: "" },
        updatedAt = updatedAt.ifBlank { updatedAtSnake ?: "" },
        completedAt = completedAt ?: completedAtSnake,
        endedAt = endedAt ?: endedAtSnake,
        discardedAt = discardedAt ?: discardedAtSnake,
    )

    private fun EkagraAnalyticsStatsDto.toDomain() = EkagraAnalyticsStats(
        totalFocusMinutes = totalFocusMinutes ?: 0,
        totalBreakMinutes = totalBreakMinutes ?: 0,
        timerUsageCount = timerUsageCount ?: 0,
        breakSessionsCount = breakSessionsCount ?: 0,
        shortBreakSessionsCount = shortBreakSessionsCount ?: 0,
        longBreakSessionsCount = longBreakSessionsCount ?: 0,
        longDurationSessionCount = longDurationSessionCount ?: 0,
        averageTimerMinutes = averageTimerMinutes ?: 0,
        mostUsedTimerDurationMinutes = mostUsedTimerDurationMinutes,
        totalSessions = totalSessions ?: 0,
        completedSessions = completedSessions ?: 0,
        endedEarlySessions = endedEarlySessions ?: 0,
        abandonedSessions = abandonedSessions ?: 0,
        weeklyData = weeklyData ?: List(7) { 0 },
        weeklyBreaks = weeklyBreaks ?: List(7) { 0 },
        focusStreak = focusStreak ?: 0,
        hourlyDistribution = hourlyDistribution ?: List(24) { 0 },
        recentSessions = recentSessions.orEmpty().map { it.toDomain() },
        focusSessions = focusSessions.orEmpty().map { it.toDomain() },
    )

    private fun EkagraAnalyticsRecentSessionDto.toDomain() = EkagraAnalyticsRecentSession(
        id = id ?: "",
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes ?: 0,
        actualMinutes = actualMinutes ?: 0,
        completed = completed ?: false,
        taskText = taskText,
        associatedGoalId = associatedGoalId,
        pauseCount = pauseCount ?: 0,
        sessionType = sessionType ?: "focus",
    )

    private fun EkagraAnalyticsFocusSessionDto.toDomain() = EkagraAnalyticsFocusSession(
        id = id ?: "",
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes ?: 0,
        actualMinutes = actualMinutes ?: 0,
        status = status ?: "completed",
        rawStatus = rawStatus ?: "completed",
        taskText = taskText,
        associatedGoalId = associatedGoalId,
        pauseCount = pauseCount ?: 0,
    )
}
