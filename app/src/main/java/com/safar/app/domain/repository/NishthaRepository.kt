package com.safar.app.domain.repository

import com.safar.app.domain.model.Mood
import com.safar.app.util.Resource

interface NishthaRepository {
    suspend fun getMoods(): Resource<List<Mood>>
    suspend fun createMood(mood: String, intensity: Int, notes: String?): Resource<Mood>
}
