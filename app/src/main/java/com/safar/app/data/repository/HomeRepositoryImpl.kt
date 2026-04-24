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
        title: String, description: String?, priority: String,
        scheduledDate: String, startedAt: String, subtasks: List<String>
    ): Resource<Goal> =
        safeApiCall {
            homeApi.addGoal(AddGoalRequest(text = title, title = title, description = description, subtasks = subtasks, scheduledDate = scheduledDate, startedAt = startedAt))
        }.map { it.toDomain() }

    override suspend fun updateGoal(id: String, title: String, description: String?, priority: String): Resource<Goal> =
        safeApiCall { homeApi.updateGoal(id, UpdateGoalRequest(title, description, priority)) }
            .map { it.toDomain() }

    override suspend fun completeGoal(id: String, studiedMinutes: Int): Resource<Unit> {
        val completedAt = java.time.Instant.now().toString()
        return safeApiCall { homeApi.completeGoal(id, CompleteGoalRequest(completedAt = completedAt, studiedMinutes = studiedMinutes)) }
            .map { }
    }

    override suspend fun deleteGoal(id: String): Resource<Unit> =
        safeApiCall { homeApi.deleteGoal(id) }.map { }

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
        title           = title ?: text ?: "",
        description     = description,
        category        = category ?: "other",
        priority        = priority ?: "medium",
        completed       = completed ?: false,
        completedAt     = completedAt ?: completedAtSnake,
        scheduledDate   = scheduledDate ?: scheduledDateSnake,
        lifecycleStatus = lifecycleStatus ?: lifecycleStatusSnake,
        type            = type,
        startedAt       = startedAt
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
