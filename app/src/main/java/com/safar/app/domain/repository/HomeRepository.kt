package com.safar.app.domain.repository

import com.safar.app.domain.model.*
import com.safar.app.util.Resource

interface HomeRepository {
    suspend fun getStreaks(): Resource<Streaks>
    suspend fun getMoods(): Resource<List<Mood>>
    suspend fun getGoals(): Resource<List<Goal>>
    suspend fun addGoal(title: String, description: String?, priority: String, scheduledDate: String, startedAt: String, subtasks: List<String>): Resource<Goal>
    suspend fun updateGoal(id: String, title: String, description: String?, priority: String): Resource<Goal>
    suspend fun completeGoal(id: String, studiedMinutes: Int): Resource<Unit>
    suspend fun deleteGoal(id: String): Resource<Unit>
    suspend fun getMonthlyReport(): Resource<MonthlyReport>
    suspend fun generateMonthlyReport(month: String): Resource<MonthlyReport>
    suspend fun getActiveTitle(): Resource<ActiveTitle>
    suspend fun getAchievements(): Resource<List<Achievement>>
    suspend fun getLoginHistory(): Resource<List<LoginHistoryEntry>>
}
