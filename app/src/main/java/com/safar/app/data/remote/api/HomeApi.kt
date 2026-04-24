package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface HomeApi {
    @GET("streaks") suspend fun getStreaks(): Response<StreaksDto>
    @GET("moods") suspend fun getMoods(): Response<List<MoodDto>>
    @GET("goals") suspend fun getGoals(): Response<List<GoalDto>>
    @POST("goals") suspend fun addGoal(@Body request: AddGoalRequest): Response<GoalDto>
    @PATCH("goals/{id}") suspend fun updateGoal(@Path("id") id: String, @Body request: UpdateGoalRequest): Response<GoalDto>
    @PATCH("goals/{id}") suspend fun completeGoal(@Path("id") id: String, @Body request: CompleteGoalRequest): Response<GoalDto>
    @DELETE("goals/{id}") suspend fun deleteGoal(@Path("id") id: String): Response<Unit>
    @GET("analytics/monthly-report") suspend fun getMonthlyReport(): Response<MonthlyReportDto>
    @POST("analytics/monthly-report/generate") suspend fun generateMonthlyReport(@Body request: GenerateReportRequest): Response<MonthlyReportDto>
    @GET("achievements/active-title") suspend fun getActiveTitle(): Response<ActiveTitleDto>
    @GET("achievements/all") suspend fun getAchievements(): Response<AchievementsResponse>
}
