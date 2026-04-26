package com.safar.app.data.remote.api

import com.safar.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface HomeApi {
    @GET("streaks") suspend fun getStreaks(): Response<StreaksDto>
    @GET("moods") suspend fun getMoods(): Response<List<MoodDto>>
    @GET("goals") suspend fun getGoals(): Response<List<GoalDto>>
    @POST("goals") suspend fun addGoal(@Body request: AddGoalRequest): Response<GoalDto>
    @PATCH("goals/{id}") suspend fun updateGoal(@Path("id") id: String, @Body request: UpdateGoalRequest): Response<BasicMessageResponse>
    @PATCH("goals/{id}") suspend fun completeGoal(@Path("id") id: String, @Body request: CompleteGoalRequest): Response<BasicMessageResponse>
    @DELETE("goals/{id}") suspend fun deleteGoal(@Path("id") id: String): Response<Unit>
    @POST("goals/{id}/repeat") suspend fun repeatGoal(@Path("id") id: String, @Body request: RepeatGoalRequest): Response<GoalDto>
    @GET("goals/previous-goals") suspend fun getPreviousGoals(@Query("period") period: String, @Query("days") days: Int? = null): Response<List<GoalDto>>
    @POST("goals/repeat-plan") suspend fun repeatPlan(@Body request: RepeatPlanRequest): Response<RepeatPlanResponse>
    @GET("goals/rollover-prompts") suspend fun getRolloverPrompts(): Response<List<GoalDto>>
    @POST("goals/{id}/rollover-action") suspend fun rolloverAction(@Path("id") id: String, @Body request: RolloverActionRequest): Response<RolloverActionResponse>
    @POST("goals/focus-summary") suspend fun getGoalFocusSummary(@Body request: FocusSummaryRequest): Response<GoalFocusSummaryResponse>
    @GET("ekagra-sessions/analytics") suspend fun getEkagraAnalytics(): Response<EkagraAnalyticsStatsDto>
    @GET("analytics/monthly-report") suspend fun getMonthlyReport(): Response<MonthlyReportDto>
    @POST("analytics/monthly-report/generate") suspend fun generateMonthlyReport(@Body request: GenerateReportRequest): Response<MonthlyReportDto>
    @GET("achievements/active-title") suspend fun getActiveTitle(): Response<ActiveTitleDto>
    @GET("achievements/all") suspend fun getAchievements(): Response<AchievementsResponse>
}
