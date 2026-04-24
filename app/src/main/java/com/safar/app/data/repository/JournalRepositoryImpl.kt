package com.safar.app.data.repository

import com.safar.app.data.remote.api.JournalApi
import com.safar.app.data.remote.dto.CreateJournalRequest
import com.safar.app.data.remote.dto.JournalDto
import com.safar.app.domain.model.JournalEntry
import com.safar.app.domain.repository.JournalRepository
import com.safar.app.util.Resource
import com.safar.app.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepositoryImpl @Inject constructor(
    private val journalApi: JournalApi
) : JournalRepository {

    override suspend fun getJournals(): Resource<List<JournalEntry>> {
        val result = safeApiCall { journalApi.getJournals() }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    override suspend fun createJournal(content: String, title: String?, moodTag: String?): Resource<JournalEntry> {
        val result = safeApiCall { journalApi.createJournal(CreateJournalRequest(content, title, moodTag)) }
        return when (result) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error   -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
    }

    private fun JournalDto.toDomain() = JournalEntry(
        id        = id        ?: mongoId ?: "",
        content   = content   ?: "",
        timestamp = timestamp ?: ""
    )
}
