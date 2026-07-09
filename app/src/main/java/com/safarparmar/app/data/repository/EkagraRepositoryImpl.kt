package com.safarparmar.app.data.repository

import com.safarparmar.app.data.remote.api.FocusApi
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsFocusSessionDto
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsRecentSessionDto
import com.safarparmar.app.data.remote.dto.EkagraAnalyticsStatsDto
import com.safarparmar.app.data.remote.dto.EkagraTimerDurationUsageDto
import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.data.remote.dto.LinkedEkagraSessionDto
import com.safarparmar.app.data.remote.dto.SaveEkagraSessionRequest
import com.safarparmar.app.data.remote.dto.TopicLinkedSessionDto
import com.safarparmar.app.domain.model.EkagraAnalyticsFocusSession
import com.safarparmar.app.domain.model.EkagraAnalyticsRecentSession
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.domain.model.EkagraTimerDurationUsage
import com.safarparmar.app.domain.model.GoalLinkedSession
import com.safarparmar.app.domain.model.TopicLinkedSession
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

    override suspend fun getLinkedSessions(): Resource<List<GoalLinkedSession>> {
        return try {
            val res = focusApi.getLinkedSessions()
            if (res.isSuccessful) Resource.Success(res.body()?.sessions.orEmpty().map { it.toDomain() })
            else Resource.Error("Linked sessions failed: ${res.code()}")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun getTopicLinkedSessions(planId: String?): Resource<List<TopicLinkedSession>> {
        return try {
            val res = focusApi.getTopicLinkedSessions(planId)
            if (res.isSuccessful) Resource.Success(res.body()?.sessions.orEmpty().map { it.toDomain() })
            else Resource.Error("Topic-linked sessions failed: ${res.code()}")
        } catch (e: Exception) {
            Resource.Error("Network error: ${e.message}")
        }
    }

    override suspend fun saveSession(
        clientSessionId: String?,
        mode: String,
        startedAt: String,
        endedAt: String?,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        actualDurationSeconds: Int?,
        goalId: String?,
        goalTitle: String?,
        topicId: String?,
        planId: String?,
        topicTitle: String?,
        markTopicDone: Boolean,
        taskTitle: String?,
        markGoalComplete: Boolean,
        shieldEnabled: Boolean,
    ): Resource<EkagraSession> {
        return try {
            val res = focusApi.saveSession(
                SaveEkagraSessionRequest(
                    clientSessionId = clientSessionId,
                    mode = mode,
                    startedAt = startedAt,
                    endedAt = endedAt,
                    plannedDurationMinutes = plannedDurationMinutes,
                    actualDurationMinutes = actualDurationMinutes,
                    actualDurationSeconds = actualDurationSeconds ?: (actualDurationMinutes * 60),
                    goalId = goalId,
                    goalTitle = goalTitle,
                    topicId = topicId,
                    planId = planId,
                    topicTitle = topicTitle,
                    markTopicDone = markTopicDone,
                    taskTitle = taskTitle,
                    markGoalComplete = markGoalComplete,
                    kavachEnabled = shieldEnabled,
                    shieldEnabled = shieldEnabled,
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
        goalLinkedTime = goalLinkedTime ?: 0,
        topicLinkedTime = topicLinkedTime ?: 0,
        untitledTime = untitledTime ?: 0,
        goalLinkedSessionCount = goalLinkedSessionCount ?: 0,
        topicLinkedSessionCount = topicLinkedSessionCount ?: 0,
        untitledSessionCount = untitledSessionCount ?: 0,
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
        actualSeconds = actualSeconds ?: 0,
        completed = completed ?: false,
        taskText = taskText,
        associatedGoalId = associatedGoalId,
        isGoalLinked = isGoalLinked ?: false,
        pauseCount = pauseCount ?: 0,
        sessionType = sessionType ?: "ekagra",
    )

    private fun EkagraAnalyticsFocusSessionDto.toDomain() = EkagraAnalyticsFocusSession(
        id = id ?: "",
        startedAt = startedAt,
        endedAt = endedAt,
        durationMinutes = durationMinutes ?: 0,
        actualMinutes = actualMinutes ?: 0,
        actualSeconds = actualSeconds ?: 0,
        status = status ?: "completed",
        rawStatus = rawStatus ?: "completed",
        taskText = taskText,
        associatedGoalId = associatedGoalId,
        isGoalLinked = isGoalLinked ?: false,
        pauseCount = pauseCount ?: 0,
        timerMode = timerMode,
    )

    private fun LinkedEkagraSessionDto.toDomain() = GoalLinkedSession(
        id = id,
        goalId = goalId,
        goalTitle = goalTitle,
        goalExists = goalExists,
        durationMinutes = durationMinutes,
        durationSeconds = durationSeconds,
        startedAt = startedAt,
        endedAt = endedAt,
        timerMode = timerMode,
        source = source,
    )

    private fun TopicLinkedSessionDto.toDomain() = TopicLinkedSession(
        id = id,
        topicId = topicId,
        planId = planId,
        topicTitle = topicTitle,
        topicExists = topicExists,
        durationMinutes = durationMinutes,
        durationSeconds = durationSeconds,
        startedAt = startedAt,
        endedAt = endedAt,
        timerMode = timerMode,
        source = source,
    )
}
