package com.safar.app.data.repository

import com.safar.app.data.remote.api.AuthApi
import com.safar.app.data.remote.api.HomeApi
import com.safar.app.data.remote.dto.*
import com.safar.app.domain.model.*
import com.safar.app.domain.repository.HomeRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepositoryImpl @Inject constructor(
    private val homeApi: HomeApi,
    private val authApi: AuthApi
) : HomeRepository {

    override suspend fun getStreaks(): Resource<Streaks> =
        safeApiCall { homeApi.getStreaks() }.map { it.toDomain() }

    override suspend fun getMoods(): Resource<List<Mood>> =
        safeApiCall { homeApi.getMoods() }.map { list -> list.map { it.toDomain() } }

    override suspend fun getGoals(): Resource<List<Goal>> =
        safeApiCall { homeApi.getGoals() }.map { list -> list.map { it.toDomain() } }

    override suspend fun addGoal(
        title: String,
        description: String?,
        priority: String,
        scheduledDate: String?,
        startedAt: String?,
        subtasks: List<GoalSubtask>,
        goalKind: String,
        unitType: String,
        linkedFocusEnabled: Boolean,
        plannedFocusMinutes: Int?,
        targetValue: Int?,
        achievedValue: Int?,
        status: String,
        carryForwardMode: String,
        source: String
    ): Resource<Goal> =
        safeApiCall {
            homeApi.addGoal(
                AddGoalRequest(
                    text = title,
                    title = title,
                    description = description,
                    priority = priority,
                    subtasks = subtasks.map { it.toDto() },
                    scheduledDate = scheduledDate,
                    startedAt = startedAt,
                    source = source,
                    goalKind = goalKind,
                    unitType = unitType,
                    executionMode = if (unitType == "duration_minutes" && linkedFocusEnabled) "timed" else "manual",
                    linkedFocusEnabled = if (unitType == "duration_minutes") linkedFocusEnabled else false,
                    plannedFocusMinutes = plannedFocusMinutes,
                    targetValue = targetValue,
                    achievedValue = achievedValue,
                    status = status,
                    carryForwardMode = carryForwardMode
                )
            )
        }.map { it.toDomain() }

    override suspend fun updateGoalDetails(
        id: String,
        title: String,
        description: String?,
        priority: String,
        scheduledDate: String?,
        startedAt: String?,
        subtasks: List<GoalSubtask>,
        goalKind: String,
        unitType: String,
        linkedFocusEnabled: Boolean,
        plannedFocusMinutes: Int?,
        targetValue: Int?,
        achievedValue: Int?,
        status: String,
        carryForwardMode: String
    ): Resource<Unit> =
        safeApiCall {
            homeApi.updateGoal(
                id,
                UpdateGoalRequest(
                    title = title,
                    text = title,
                    description = description,
                    priority = priority,
                    scheduledDate = scheduledDate,
                    startedAt = startedAt,
                    subtasks = subtasks.map { it.toDto() },
                    goalKind = goalKind,
                    unitType = unitType,
                    executionMode = if (unitType == "duration_minutes" && linkedFocusEnabled) "timed" else "manual",
                    linkedFocusEnabled = if (unitType == "duration_minutes") linkedFocusEnabled else false,
                    plannedFocusMinutes = plannedFocusMinutes,
                    targetValue = targetValue,
                    achievedValue = achievedValue,
                    status = status,
                    carryForwardMode = carryForwardMode
                )
            )
        }.map { }

    override suspend fun completeGoal(id: String, studiedMinutes: Int): Resource<Unit> {
        val completedAt = java.time.Instant.now().toString()
        return safeApiCall { homeApi.completeGoal(id, CompleteGoalRequest(completedAt = completedAt, studiedMinutes = studiedMinutes)) }
            .map { }
    }

    override suspend fun deleteGoal(id: String): Resource<Unit> =
        safeApiCall { homeApi.deleteGoal(id) }.map { }

    override suspend fun repeatGoal(id: String, scheduledDate: String): Resource<Goal> =
        safeApiCall { homeApi.repeatGoal(id, RepeatGoalRequest(scheduledDate)) }.map { it.toDomain() }

    override suspend fun getRolloverPrompts(): Resource<List<Goal>> =
        safeApiCall { homeApi.getRolloverPrompts() }.map { list -> list.map { it.toDomain() } }

    override suspend fun respondToRollover(id: String, action: String): Resource<GoalRolloverResult> =
        safeApiCall { homeApi.rolloverAction(id, RolloverActionRequest(action)) }
            .map { GoalRolloverResult(message = it.message ?: "", goal = it.goal?.toDomain()) }

    override suspend fun getGoalFocusSummary(goalIds: List<String>, dayKey: String?): Resource<GoalFocusSummary> {
        if (goalIds.isEmpty()) return Resource.Success(GoalFocusSummary())
        return safeApiCall { homeApi.getGoalFocusSummary(FocusSummaryRequest(goalIds, dayKey)) }
            .map { response ->
                GoalFocusSummary(
                    allTime = response.allTime.orEmpty().mapValues { it.value.toDomain() },
                    forDay = response.forDay.orEmpty().mapValues { it.value.toDomain() }
                )
            }
    }

    override suspend fun getEkagraAnalytics(): Resource<EkagraAnalyticsStats> =
        safeApiCall { homeApi.getEkagraAnalytics() }.map { it.toDomain() }

    override suspend fun getMonthlyReport(): Resource<MonthlyReport> =
        safeApiCall { homeApi.getMonthlyReport() }.map { it.toDomain() }

    override suspend fun generateMonthlyReport(month: String): Resource<MonthlyReport> =
        safeApiCall { homeApi.generateMonthlyReport(GenerateReportRequest(month)) }.map { it.toDomain() }

    override suspend fun getActiveTitle(): Resource<ActiveTitle> =
        safeApiCall { homeApi.getActiveTitle() }
            .map { ActiveTitle(title = it.title ?: "", selectedId = it.selectedId ?: "") }

    override suspend fun getAchievements(): Resource<List<Achievement>> =
        safeApiCall { homeApi.getAchievements() }
            .map { it.achievements?.map { a -> a.toDomain() } ?: emptyList() }

    override suspend fun getLoginHistory(): Resource<List<LoginHistoryEntry>> =
        safeApiCall { authApi.getLoginHistory() }
            .map { list -> list.map { LoginHistoryEntry(timestamp = it.timestamp ?: "") } }

    // ── Mappers ───────────────────────────────────────────────────────────────

    // Handles both camelCase and snake_case since the API is inconsistent
    private fun StreaksDto.toDomain() = Streaks(
        loginStreak          = loginStreak ?: loginStreakSnake ?: 0,
        checkInStreak        = checkInStreak ?: checkInStreakSnake ?: 0,
        goalCompletionStreak = goalCompletionStreak ?: goalCompletionStreakSnake ?: 0,
        lastActiveDate       = lastActiveDate ?: lastActiveDateSnake
    )

    private fun MoodDto.toDomain() = Mood(
        id        = id ?: "",
        mood      = mood ?: "",
        intensity = intensity ?: 0,
        notes     = notes,
        timestamp = timestamp ?: ""
    )

    // _id (Mongo) used as fallback; text used as fallback for title
    private fun GoalDto.toDomain() = Goal(
        id              = id ?: mongoId ?: "",
        userId          = userId ?: "",
        text            = text ?: title ?: "",
        title           = title ?: text ?: "",
        description     = description,
        source          = source ?: "manual",
        importedFromGoal = importedFromGoal ?: importedFromGoalSnake ?: false,
        completedViaFocus = completedViaFocus ?: completedViaFocusSnake ?: false,
        goalKind        = goalKind ?: goalKindSnake ?: "today",
        unitType        = unitType ?: unitTypeSnake ?: "binary",
        executionMode   = executionMode ?: executionModeSnake ?: "manual",
        linkedFocusEnabled = linkedFocusEnabled ?: linkedFocusEnabledSnake ?: false,
        plannedFocusMinutes = plannedFocusMinutes ?: plannedFocusMinutesSnake,
        targetValue     = targetValue ?: targetValueSnake,
        achievedValue   = achievedValue ?: achievedValueSnake ?: if (completed == true) 1 else 0,
        status          = status ?: statusSnake ?: if (completed == true) "completed" else "not_started",
        carryForwardMode = carryForwardMode ?: carryForwardModeSnake ?: if ((goalKind ?: goalKindSnake) == "repeat") "ask" else "none",
        category        = category ?: "other",
        priority        = priority ?: "medium",
        completed       = completed ?: false,
        createdAt       = createdAt ?: createdAtSnake,
        completedAt     = completedAt ?: completedAtSnake,
        studiedMinutes  = studiedMinutes ?: studiedMinutesSnake,
        scheduledDate   = scheduledDate ?: scheduledDateSnake,
        startedAt       = startedAtCamel ?: startedAt,
        expiresAt       = expiresAtCamel ?: expiresAt,
        lifecycleStatus = lifecycleStatus ?: lifecycleStatusSnake,
        rolloverPromptPending = rolloverPromptPendingSnake ?: false,
        sourceGoalId    = sourceGoalIdSnake,
        type            = type,
        subtasks        = subtasks?.mapNotNull { it.toGoalSubtask() } ?: emptyList()
    )

    private fun GoalSubtask.toDto() = GoalSubtaskDto(id = id, text = text, done = done)

    private fun GoalSubtaskDto.toDomain() = GoalSubtask(
        id = id ?: text ?: "",
        text = text ?: "",
        done = done ?: false
    )

    private fun Any.toGoalSubtask(): GoalSubtask? = when (this) {
        is GoalSubtaskDto -> toDomain()
        is String -> GoalSubtask(id = this, text = this, done = false)
        is Map<*, *> -> {
            val text = this["text"]?.toString().orEmpty()
            GoalSubtask(
                id = this["id"]?.toString() ?: text,
                text = text,
                done = this["done"] as? Boolean ?: false
            )
        }
        else -> null
    }

    private fun GoalFocusSummaryItemDto.toDomain() = GoalFocusStats(
        totalMinutes = totalMinutes ?: 0,
        sessionCount = sessionCount ?: 0
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
        abandonedSessions = abandonedSessions ?: endedEarlySessions ?: 0,
        weeklyData = weeklyData?.takeIf { it.size == 7 } ?: List(7) { 0 },
        weeklyBreaks = weeklyBreaks?.takeIf { it.size == 7 } ?: List(7) { 0 },
        focusStreak = focusStreak ?: 0,
        hourlyDistribution = hourlyDistribution?.takeIf { it.size == 24 } ?: List(24) { 0 },
        recentSessions = recentSessions?.map { it.toDomain() } ?: emptyList(),
        focusSessions = focusSessions?.map { it.toDomain() } ?: emptyList()
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
        sessionType = sessionType ?: "focus"
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
        pauseCount = pauseCount ?: 0
    )

    private fun MonthlyReportDto.toDomain(): MonthlyReport {
        val s = executiveSummary
        val i = insights
        return MonthlyReport(
            month                 = month ?: "",
            generatedAt           = generatedAt ?: "",
            consistencyScore      = s?.consistencyScore ?: 0.0,
            completionRate        = s?.completionRate ?: 0.0,
            focusDepth            = s?.focusDepth ?: 0.0,
            daysLoggedIn          = s?.daysLoggedIn ?: 0,
            daysInMonth           = s?.daysInMonth ?: 31,
            goalsCreated          = s?.goalsCreated ?: 0,
            goalsCompleted        = s?.goalsCompleted ?: 0,
            totalFocusMinutes     = s?.totalFocusMinutes ?: 0,
            consistencyMessage    = s?.consistencyMessage ?: "",
            completionMessage     = s?.completionMessage ?: "",
            focusMessage          = s?.focusMessage ?: "",
            powerHourMessage      = i?.powerHour?.message ?: "",
            moodConnectionMessage = i?.moodConnection?.message ?: "",
            sundayScariesMessage  = i?.sundayScaries?.message ?: "",
            radar                 = radar?.map { RadarItem(it.subject ?: "", it.score ?: 0.0, it.fullMark ?: 100) } ?: emptyList(),
            heatmap               = heatmap?.map { HeatmapDay(it.date ?: "", it.dayOfWeek ?: "", it.value ?: 0, it.intensity ?: 0) } ?: emptyList()
        )
    }

    private fun AchievementDto.toDomain() = Achievement(
        id           = id ?: "",
        name         = name ?: "",
        description  = description,
        type         = type ?: "",
        category     = category ?: "",
        rarity       = rarity,
        tier         = tier,
        requirement  = requirement ?: "",
        holderCount  = holderCount ?: 0,
        earned       = earned ?: false,
        progress     = progress ?: 0,
        currentValue = currentValue ?: 0,
        targetValue  = targetValue ?: 0
    )
}

// Extension to reduce Resource when/map boilerplate
private fun <T, R> Resource<T>.map(transform: (T) -> R): Resource<R> = when (this) {
    is Resource.Success -> Resource.Success(transform(data))
    is Resource.Error   -> Resource.Error(message)
    is Resource.Loading -> Resource.Loading()
}
