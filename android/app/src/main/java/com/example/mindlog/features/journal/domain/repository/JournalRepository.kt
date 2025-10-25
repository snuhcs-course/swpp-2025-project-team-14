package com.example.mindlog.features.journal.domain.repository

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest

interface JournalRepository {

    suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    )

    suspend fun getJournals(limit: Int, cursor: Int?): JournalListResponse

    /**
     * ID로 특정 일기를 조회한다.
     */
    suspend fun getJournalById(journalId: Int): JournalItemResponse

    /**
     * 일기를 수정한다.
     */
    suspend fun updateJournal(
        journalId: Int,
        request: UpdateJournalRequest
    )

    /**
     * 일기를 삭제한다.
     */
    suspend fun deleteJournal(journalId: Int)
}
