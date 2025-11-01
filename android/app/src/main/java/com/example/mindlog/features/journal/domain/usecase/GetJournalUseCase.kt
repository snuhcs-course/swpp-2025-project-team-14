package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 목록 조회' 비즈니스 로직을 처리하는 UseCase.
 */
class GetJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    // 1. limit과 cursor를 파라미터로 받도록 수정
    // 2. Repository의 응답을 그대로 반환하도록 수정
    suspend operator fun invoke(limit: Int, cursor: Int?): JournalListResponse {
        return repository.getJournals(limit = limit, cursor = cursor)
    }
}
