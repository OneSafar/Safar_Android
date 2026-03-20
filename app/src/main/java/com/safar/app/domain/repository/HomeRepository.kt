package com.safar.app.domain.repository

import com.safar.app.domain.model.*
import com.safar.app.util.Resource

interface HomeRepository {
    suspend fun getStreaks(): Resource<Streaks>
    suspend fun getMoods(): Resource<List<Mood>>
    suspend fun getGoals(): Resource<List<Goal>>
    suspend fun getMonthlyReport(): Resource<MonthlyReport>
    suspend fun getActiveTitle(): Resource<ActiveTitle>
    suspend fun getAchievements(): Resource<List<Achievement>>
}