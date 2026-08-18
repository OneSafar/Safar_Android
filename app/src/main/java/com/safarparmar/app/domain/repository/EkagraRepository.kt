package com.safarparmar.app.domain.repository

import com.safarparmar.app.data.remote.dto.EkagraSession
import com.safarparmar.app.data.remote.dto.FocusStatsResponse
import com.safarparmar.app.domain.model.EkagraAnalyticsStats
import com.safarparmar.app.util.Resource

interface EkagraRepository {
    suspend fun getStats(): Resource<FocusStatsResponse>
    suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats>
    suspend fun saveSession(
        clientSessionId: String? = null,
        mode: String,
        startedAt: String,
        endedAt: String?,
        plannedDurationMinutes: Int,
        actualDurationMinutes: Int,
        actualDurationSeconds: Int? = null,
        goalId: String? = null,
        goalTitle: String? = null,
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
        markTopicDone: Boolean = false,
        taskTitle: String? = null,
        markGoalComplete: Boolean = false,
        shieldEnabled: Boolean = false,
    ): Resource<EkagraSession>
    suspend fun deleteSession(sessionId: String): Resource<Unit>
    /**
     * Renames / re-associates an existing session IN PLACE (keeps its id). Only
     * non-null args are sent, so a title-only rename won't clear a goal/topic link.
     * This is the correct path for editing history sessions — never save+delete.
     */
    suspend fun updateSession(
        sessionId: String,
        taskTitle: String? = null,
        goalId: String? = null,
        goalTitle: String? = null,
        topicId: String? = null,
        planId: String? = null,
        topicTitle: String? = null,
    ): Resource<Unit>
    suspend fun linkSessionToGoal(
        sessionId: String,
        goalId: String,
        markGoalComplete: Boolean,
    ): Resource<Unit>
    suspend fun getLinkedSessions(): Resource<List<com.safarparmar.app.domain.model.GoalLinkedSession>>
    suspend fun getTopicLinkedSessions(planId: String? = null): Resource<List<com.safarparmar.app.domain.model.TopicLinkedSession>>
}
