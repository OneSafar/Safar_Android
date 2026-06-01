package com.safarparmar.app.domain.repository

import com.safarparmar.app.domain.model.Mood
import com.safarparmar.app.util.Resource

interface NishthaRepository {
    suspend fun getMoods(): Resource<List<Mood>>
    suspend fun createMood(mood: String, intensity: Int, notes: String?): Resource<Mood>
}
