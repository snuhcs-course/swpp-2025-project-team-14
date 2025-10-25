package com.example.mindlog.features.journal.domain.repository

import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.data.dto.JournalListResponse

/**
 * Journal 관련 데이터 처리를 위한 인터페이스.
 * Domain Layer에 위치하여 하위 Data Layer의 구체적인 구현(네트워크, DB 등)을 숨긴다.
 * ViewModel은 이 인터페이스에만 의존해야 한다.
 */
interface JournalRepository {

    /**
     * 새로운 일기를 생성한다.
     * @param title 일기 제목
     * @param content 일기 내용
     * @param emotions 감정 점수 맵 (예: "happy" to 3)
     * @param gratitude 감사한 일
     */
    suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    )

    /**
     * 일기 목록을 조회한다.
     * DTO를 앱 전체에서 사용하는 UI 모델(JournalEntry)로 변환하여 반환한다.
     * @return JournalEntry의 리스트
     */
    suspend fun getJournals(limit: Int, cursor: Int?): JournalListResponse
}
