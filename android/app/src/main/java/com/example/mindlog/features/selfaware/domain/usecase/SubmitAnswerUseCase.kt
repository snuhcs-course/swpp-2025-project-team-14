package com.example.mindlog.features.selfaware.domain.usecase

import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import javax.inject.Inject

class SubmitAnswerUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(questionId: Int, answer: String) = repo.submitAnswer(questionId, answer)
}