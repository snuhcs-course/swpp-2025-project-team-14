package com.example.mindlog.features.journal.data.repository

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.data.dto.JournalRequest
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalApi: JournalApi
) : JournalRepository {

    override suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    ) {
        val request = JournalRequest(
            title = title,
            content = content,
            emotions = emotions,
            gratitude = gratitude
        )
        journalApi.createJournal(request)
    }

    override suspend fun getJournals(limit: Int, cursor: Int?): JournalListResponse {
        return journalApi.getJournals(limit = limit, cursor = cursor)
    }

    override suspend fun getJournalById(journalId: Int): JournalItemResponse {
        return journalApi.getJournalById(journalId = journalId)
    }

    override suspend fun updateJournal(
        journalId: Int,
        request: UpdateJournalRequest
    ) {
        journalApi.updateJournal(journalId = journalId, request = request)
    }

    override suspend fun deleteJournal(journalId: Int) {
        journalApi.deleteJournal(journalId = journalId)
    }
}
