package com.example.mindlog.features.journal.data.api

import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.journal.data.dto.JournalRequest
import com.example.mindlog.features.journal.data.dto.JournalResponse
import com.example.mindlog.features.journal.data.dto.UpdateJournalRequest // 1. 수정 요청 DTO import
import retrofit2.Response // 2. Retrofit Response import
import retrofit2.http.*

interface JournalApi {

    @POST("journal/")
    suspend fun createJournal(
        @Body request: JournalRequest
    ): JournalResponse

    @GET("journal/me")
    suspend fun getJournals(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: Int?
    ): JournalListResponse

    /**
     * 특정 ID의 일기 상세 정보를 조회합니다. (GET /journal/{id})
     */
    @GET("journal/{journal_id}")
    suspend fun getJournalById(@Path("journal_id") journalId: Int): JournalItemResponse

    /**
     * 특정 ID의 일기를 수정합니다. (PATCH /journal/{id})
     * @param journalId 수정할 일기의 ID
     * @param request 수정할 내용이 담긴 요청 객체
     */
    @PATCH("journal/{journal_id}")
    suspend fun updateJournal(
        @Path("journal_id") journalId: Int,
        @Body request: UpdateJournalRequest
    ): Response<String> // 3. "Update Success" 문자열을 받기 위해 Response<String> 사용

    /**
     * 특정 ID의 일기를 삭제합니다. (DELETE /journal/{id})
     * @param journalId 삭제할 일기의 ID
     */
    @DELETE("journal/{journal_id}")
    suspend fun deleteJournal(
        @Path("journal_id") journalId: Int
    ): Response<String> // 4. "Delete Success" 문자열을 받기 위해 Response<String> 사용
}
