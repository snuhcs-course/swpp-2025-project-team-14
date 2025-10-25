package com.example.mindlog.features.selfaware.domain.usecase

import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import java.time.LocalDate
import javax.inject.Inject

class GetTodayQAUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(date: LocalDate) = repo.getTodayQA(date)
}
class SubmitAnswerUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(questionId: Int, answer: String) = repo.submitAnswer(questionId, answer)
}
class GetQAHistoryUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(cursor: Int, size: Int) = repo.getQAHistory(cursor, size)
}

class GetTopValueScoresUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(@Suppress("UNUSED_PARAMETER") unit: Unit = Unit) = repo.getTopValueScores()
}

class GetValueMapUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(@Suppress("UNUSED_PARAMETER") unit: Unit = Unit) = repo.getValueMap()
}

class GetPersonalityInsightUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(@Suppress("UNUSED_PARAMETER") unit: Unit = Unit) = repo.getPersonalityInsight()
}