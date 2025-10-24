package com.example.mindlog.features.journal.data.repository

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.JournalRequest
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

/**
 * JournalRepository의 구현체.
 * Data Layer에 위치하며, Retrofit API(JournalApi)를 사용하여 원격 서버와 통신한다.
 */
class JournalRepositoryImpl @Inject constructor(
    private val journalApi: JournalApi // 1. Hilt가 JournalApi의 구현체를 주입해준다.
) : JournalRepository {

    /**
     * JournalRepository 인터페이스의 createJournal 함수를 실제로 구현하는 부분.
     */
    override suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String?
    ) {
        // 2. 서버 API 명세에 맞춰 요청(Request) 객체를 생성한다.
        val request = JournalRequest(
            title = title,
            content = content,
            // 백엔드 요구사항: 점수가 0인 감정은 요청 본문에서 제외
            emotions = emotions.filter { it.value > 0 },
            gratitude = gratitude
        )

        // 3. API 인터페이스의 함수를 호출하여 실제 네트워크 요청을 실행한다.
        // 반환 값은 현재 사용하지 않으므로 변수에 할당하지 않아도 된다.
        journalApi.createJournal(request)
    }
}
