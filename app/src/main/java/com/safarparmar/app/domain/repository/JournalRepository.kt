package com.safarparmar.app.domain.repository

import com.safarparmar.app.domain.model.JournalEntry
import com.safarparmar.app.util.Resource

interface JournalRepository {
    suspend fun getJournals(): Resource<List<JournalEntry>>
    suspend fun createJournal(content: String, title: String?, moodTag: String?): Resource<JournalEntry>
}
