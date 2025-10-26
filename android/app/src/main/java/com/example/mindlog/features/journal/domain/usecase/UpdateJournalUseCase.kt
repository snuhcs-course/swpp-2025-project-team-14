package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 수정' 비즈니스 로직을 처리하는 UseCase.
 * 원본 데이터와 비교하여 변경된 필드만 요청에 포함시킨다.
 */
class UpdateJournalUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(
        journalId: Int,
        originalJournal: JournalItemResponse, // 1. 원본 데이터
        newTitle: String,                     // 2. 수정된 데이터
        newContent: String,
        newGratitude: String
    ) {
        // 3. 원본과 비교하여 변경된 값만 DTO에 할당, 아니면 null 할당
        val titleToUpdate = if (originalJournal.title != newTitle) newTitle else null
        val contentToUpdate = if (originalJournal.content != newContent) newContent else null
        val gratitudeToUpdate = if (originalJournal.gratitude != newGratitude) newGratitude else null

        // 아무것도 변경되지 않았으면 요청을 보내지 않음 (선택적 최적화)
        if (titleToUpdate == null && contentToUpdate == null && gratitudeToUpdate == null) {
            return // 변경사항 없으면 함수 종료
        }

        // 4. 변경된 필드만 포함할 수 있는 DTO 객체 생성
        val request = UpdateJournalRequest(
            title = titleToUpdate,
            content = contentToUpdate,
            summary = null, // summary는 항상 null로 고정
            gratitude = gratitudeToUpdate
        )

        repository.updateJournal(
            journalId = journalId,
            request = request
        )
    }
}
