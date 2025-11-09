package com.example.mindlog.features.selfaware.domain.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Paged
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.model.TodayQA
import java.time.LocalDate

interface SelfAwareRepository {
    suspend fun getTodayQA(date: LocalDate): Result<TodayQA>
    suspend fun submitAnswer(questionId: Int, answer: String): Result<Answer>
    suspend fun getQAHistory(
        cursor: Int, size: Int
    ): Result<Paged<QAItem>>
}
