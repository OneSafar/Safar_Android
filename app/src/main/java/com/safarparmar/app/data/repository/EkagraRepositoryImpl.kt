package com.safarparmar.app.data.repository

import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsFocusSessionDto
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsRecentSessionDto
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safarparmar.app.data.remote.dto.EkagraTimerDurationUsageDto
import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsRecentSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.EkagraTimerDurationUsage
import com.safarparmar.app.domain.repository.EkagraRepository
import com.safarparmar.app.util.Resource
import com.safarparmar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EkagraRepositoryImpl @Inject constructor(
    private val focusApi: FocusApi,
) : EkagraRepository {

    override suspend fun getStats(): Resource<FocusStatsResponse> {
        return safeApiCall { focusApi.getStats() }
    }

    override suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats> {
        return try {
            val res = focusApi.getEkagraAnalytics()
            if (res.isSuccessful) Resource.Success((res.body() ?: EkagraAnalyticsStatsDto()).toDomain())
            else Resource.Error("Analytics failed: ${res.code()}")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun saveSession(
        mode: String,
        startedAt: String,
        endedAt: String?,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        goalId: String?,
        goalTitle: String?,
        taskTitle: String?,
        markGoalComplete: Boolean,
    ): Resource<EkagraSession> {
        return try {
            val res = focusApi.saveSession(
                SaveEkagraSessionRequest(
                    mode = mode,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    plannedDurationMinutes = plannedDurationMinutes,
                    actualDurationMinutes = actualDurationMinutes,
                    goalId = goalId,
                    goalTitle = goalTitle,
                    taskTitle = taskTitle,
                    markGoalComplete = markGoalComplete,
                ),
            )
            if (res.isSuccessful) {
                val session = res.body()?.session
                    ?: return Resource.Error("Save failed: empty response body")
                Resource.Success(session.normalized())
            } else {
                Resource.Error("Save failed: ${res.code()}")
            }
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun deleteSession(sessionId: String): Resource<Unit> {
        return try {
            val res = focusApi.deleteSession(sessionId)
            if (res.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Delete failed: ${res.code()}")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
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
        timerDurationUsage = timerDurationUsage.orEmpty().map { it.toDomain() },
    )

    private fun EkagraTimerDurationUsageDto.toDomain() = EkagraTimerDurationUsage(
        durationMinutes = durationMinutes ?: 0,
        count = count ?: 0,
        sessionType = sessionType ?: "ekagra",
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
        sessionType = sessionType ?: "ekagra",
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
