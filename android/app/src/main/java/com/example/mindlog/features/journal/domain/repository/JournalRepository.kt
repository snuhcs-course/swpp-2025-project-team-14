package com.example.mindlog.features.journal.domain.repository

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalResponse
import com.example.mindlog.features.journal.data.dto.KeywordListResponse
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.data.dto.JournalListResponse

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
     * 이미지 업로드 전체 과정을 처리합니다.     * @param journalId 일기 ID
     * @param imageBytes 이미지의 ByteArray
     * @param contentType 이미지의 MIME 타입 (e.g., "image/jpeg")
     * @param fileName 파일 이름
     */
    suspend fun uploadJournalImage(
        journalId: Int,
        imageBytes: ByteArray,
        contentType: String,
        fileName: String
    )

    suspend fun searchJournals(
        startDate: String?,
        endDate: String?,
        title: String?,
        limit: Int,
        cursor: Int?
    ): JournalListResponse

    /**
     * AI 이미지 생성을 요청합니다.
     * @param style 이미지 스타일
     * @param content 이미지 생성에 사용할 텍스트
     * @return Base64로 인코딩된 이미지 문자열
     */
    suspend fun generateImage(style: String, content: String): String

    /**
     * 특정 일기의 키워드를 추출합니다.
     */
    suspend fun extractKeywords(journalId: Int): KeywordListResponse
}

