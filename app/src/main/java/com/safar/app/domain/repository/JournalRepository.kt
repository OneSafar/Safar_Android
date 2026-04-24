package com.safar.app.domain.repository

import com.safar.app.domain.model.JournalEntry
import com.safar.app.util.Resource

interface JournalRepository {
    suspend fun getJournals(): Resource<List<JournalEntry>>
    suspend fun createJournal(content: String, title: String?, moodTag: String?): Resource<JournalEntry>
}
