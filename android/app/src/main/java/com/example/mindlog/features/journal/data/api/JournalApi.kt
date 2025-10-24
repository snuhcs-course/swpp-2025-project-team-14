package com.example.mindlog.features.journal.data.api // 패키지 경로는 실제 프로젝트에 맞게 확인해주세요.

import com.example.mindlog.features.journal.data.dto.JournalRequest
import com.example.mindlog.features.journal.data.dto.JournalResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface JournalApi {

    /**
     * 새로운 일기를 생성합니다. (POST /journal)
     * @param request 생성할 일기의 제목, 내용, 감정 점수 등이 담긴 요청 객체
     * @return 생성된 일기 정보가 담긴 응답 객체
     */
    @POST("journal")
    suspend fun createJournal(
        @Body request: JournalRequest
    ): JournalResponse

    // 여기에 추후 GET, PATCH, DELETE 등 다른 Journal 관련 API 함수들이 추가될 수 있습니다.

}
