package com.safarparmar.app.data.remote.api

import com.safarparmar.app.data.remote.dto.WeeklyLeaderboardResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LeaderboardApi {
    @GET("leaderboard/weekly")
    suspend fun getWeeklyLeaderboard(
        @Query("page") page: Int = 1,
    ): Response<WeeklyLeaderboardResponseDto>
}
