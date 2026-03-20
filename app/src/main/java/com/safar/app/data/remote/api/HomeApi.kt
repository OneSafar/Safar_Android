package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface HomeApi {
    @GET("streaks")
    suspend fun getStreaks(): Response<HomeStreaksDto>

    @GET("moods")
    suspend fun getMoods(): Response<List<MoodDto>>

    @GET("goals")
    suspend fun getGoals(): Response<List<GoalDto>>

    @GET("analytics/monthly-report")
    suspend fun getMonthlyReport(): Response<MonthlyReportDto>

    @GET("achievements/active-title")
    suspend fun getActiveTitle(): Response<ActiveTitleDto>

    @GET("achievements/all")
    suspend fun getAchievements(): Response<AchievementsResponse>
}