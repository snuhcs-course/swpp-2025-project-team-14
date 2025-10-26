package com.example.mindlog.features.journal.domain.repository

import android.net.Uri
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.data.dto.JournalResponse
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest

interface JournalRepository {

    suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    ): JournalResponse

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

    /**
     * 이미지 업로드 전체 과정을 처리합니다.
     * @param journalId 일기 ID     * @param imageUri 이미지의 Uri
     */
    suspend fun uploadJournalImage(journalId: Int, imageUri: Uri)

    suspend fun searchJournals(
        startDate: String?,
        endDate: String?,
        title: String?,
        limit: Int,
        cursor: Int?
    ): JournalListResponse
}
