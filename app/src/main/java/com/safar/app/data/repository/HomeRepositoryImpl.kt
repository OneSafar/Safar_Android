package com.safar.app.data.repository

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
    private val homeApi: HomeApi
) : HomeRepository {

    override suspend fun getStreaks(): Resource<Streaks> {
        val result = safeApiCall { homeApi.getStreaks() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getMoods(): Resource<List<Mood>> {
        val result = safeApiCall { homeApi.getMoods() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getGoals(): Resource<List<Goal>> {
        val result = safeApiCall { homeApi.getGoals() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getMonthlyReport(): Resource<MonthlyReport> {
        val result = safeApiCall { homeApi.getMonthlyReport() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getActiveTitle(): Resource<ActiveTitle> {
        val result = safeApiCall { homeApi.getActiveTitle() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun getAchievements(): Resource<List<Achievement>> {
        val result = safeApiCall { homeApi.getAchievements() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.achievements.map { it.toDomain() })
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    private fun HomeStreaksDto.toDomain() = Streaks(
        loginStreak          = loginStreak,
        checkInStreak        = checkInStreak,
        goalCompletionStreak = goalCompletionStreak
    )
    private fun MoodDto.toDomain() = Mood(id, mood, intensity, notes, timestamp)

    private fun GoalDto.toDomain() = Goal(id, title, description, category, priority, completed, completedAt, scheduledDate)

    private fun MonthlyReportDto.toDomain() = MonthlyReport(
        month                = month,
        consistencyScore     = executiveSummary.consistencyScore,
        completionRate       = executiveSummary.completionRate,
        focusDepth           = executiveSummary.focusDepth,
        daysLoggedIn         = executiveSummary.daysLoggedIn,
        daysInMonth          = executiveSummary.daysInMonth,
        goalsCreated         = executiveSummary.goalsCreated,
        goalsCompleted       = executiveSummary.goalsCompleted,
        consistencyMessage   = executiveSummary.consistencyMessage,
        completionMessage    = executiveSummary.completionMessage,
        focusMessage         = executiveSummary.focusMessage,
        powerHourMessage     = insights.powerHour.message,
        radar                = radar.map { RadarItem(it.subject, it.score) }
    )

    private fun ActiveTitleDto.toDomain() = ActiveTitle(title, selectedId)

    private fun AchievementDto.toDomain() = Achievement(
        id, name, description, type, category, tier,
        requirement, holderCount, earned, progress, currentValue, targetValue
    )
}