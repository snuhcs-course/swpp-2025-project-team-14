package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * ID로 특정 일기의 상세 정보를 가져오는 UseCase.
 */
class GetJournalByIdUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(journalId: Int): JournalItemResponse {
        return repository.getJournalById(journalId)
    }
}
