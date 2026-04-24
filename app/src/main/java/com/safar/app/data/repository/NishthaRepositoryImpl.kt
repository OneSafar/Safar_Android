package com.safar.app.data.repository

import com.safar.app.data.remote.api.NishthaApi
import com.safar.app.data.remote.dto.CreateMoodRequest
import com.safar.app.data.remote.dto.MoodDto
import com.safar.app.domain.model.Mood
import com.safar.app.domain.repository.NishthaRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NishthaRepositoryImpl @Inject constructor(
    private val nishthaApi: NishthaApi
) : NishthaRepository {

    override suspend fun getMoods(): Resource<List<Mood>> {
        val result = safeApiCall { nishthaApi.getMoods() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun createMood(mood: String, intensity: Int, notes: String?): Resource<Mood> {
        val result = safeApiCall { nishthaApi.createMood(CreateMoodRequest(mood, intensity, notes)) }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    private fun MoodDto.toDomain() = Mood(
        id        = id        ?: "",
        mood      = mood      ?: "",
        intensity = intensity ?: 0,
        notes     = notes,
        timestamp = timestamp ?: ""    // ← safe: never null in domain model
    )
}
