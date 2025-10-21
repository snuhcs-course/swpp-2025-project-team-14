package com.example.mindlog.features.selfaware.data.repository

import com.example.mindlog.core.common.toResult
import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import com.example.mindlog.features.selfaware.data.dto.AnswerRequest
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

class SelfAwareRepositoryImpl @Inject constructor(
    private val api: SelfAwareApi,
    private val mapper: SelfAwareMapper,
    private val dispatcher: DispatcherProvider
) : SelfAwareRepository {

    override suspend fun getTodayQA(date: LocalDate) = withContext(dispatcher.io) {
        runCatching { mapper.toTodayQA(api.getTodayQA(date.toString())) }.toResult()
    }

    override suspend fun submitAnswer(questionId: Int, answer: String) = withContext(dispatcher.io) {
        runCatching {
            val res = api.submitAnswer(AnswerRequest(questionId, answer))
            mapper.toAnswer(res)
        }.toResult()
    }

    override suspend fun getQAHistory(cursor: Int, size: Int) = withContext(dispatcher.io) {
        runCatching {
            val dto = api.getQAHistory(cursor = cursor, size = size)
            Paged(
                items = dto.items.map(mapper::toQAItem),
                cursor = dto.cursor, size = dto.size
            )
        }.toResult()
    }
}