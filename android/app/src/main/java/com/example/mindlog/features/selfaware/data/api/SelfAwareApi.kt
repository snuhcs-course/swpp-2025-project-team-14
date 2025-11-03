package com.example.mindlog.features.selfaware.data.api

import com.example.mindlog.features.selfaware.data.dto.AnswerRequest
import com.example.mindlog.features.selfaware.data.dto.AnswerResponse
import com.example.mindlog.features.selfaware.data.dto.QAHistoryResponse
import com.example.mindlog.features.selfaware.data.dto.TodayQAResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SelfAwareApi {
    @GET("selfaware/question")
    suspend fun getTodayQA(@Query("date") isoDate: String): TodayQAResponse

    @POST("selfaware/answer")
    suspend fun submitAnswer(@Body answerRequest: AnswerRequest): AnswerResponse

    @GET("selfaware/history")
    suspend fun getQAHistory(@Query("cursor") cursor: Int, @Query("size") size: Int): QAHistoryResponse
}
