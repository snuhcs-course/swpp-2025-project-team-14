package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.KeywordResponse
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '키워드 추출' 비즈니스 로직을 처리하는 UseCase.
 */
class ExtractKeywordsUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    /**
     * @return KeywordResponse의 리스트를 반환합니다.
     */
    suspend operator fun invoke(journalId: Int): List<KeywordResponse> {
        val response = repository.extractKeywords(journalId)
        return response.data
    }
}
