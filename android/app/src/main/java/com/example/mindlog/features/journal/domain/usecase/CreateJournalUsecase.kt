package com.example.mindlog.features.journal.domain.usecase

import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * '일기 생성' 비즈니스 로직을 처리하는 UseCase.
 *
 * 이 클래스는 특정 기능(일기 생성)에 대한 책임을 가지며,
 * ViewModel로부터 요청을 받아 Repository에 데이터 처리를 위임한다.
 */
class CreateJournalUseCase @Inject constructor(
    private val repository: JournalRepository // 1. Hilt를 통해 Repository 인터페이스를 주입받음
) {

    /**
     * UseCase를 함수처럼 호출할 수 있게 해주는 'invoke' 연산자 오버로딩.
     * ViewModel에서 `createJournalUseCase(...)` 형태로 간단히 사용할 수 있다.
     *
     * @param title 일기 제목
     * @param content 일기 내용
     * @param emotions 감정 점수 맵
     * @param gratitude 감사한 일
     */
    suspend operator fun invoke(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String?
    ) {
        // 2. 주입받은 Repository의 함수를 호출하여 실제 데이터 처리를 위임한다.
        repository.createJournal(title, content, emotions, gratitude)
    }
}
