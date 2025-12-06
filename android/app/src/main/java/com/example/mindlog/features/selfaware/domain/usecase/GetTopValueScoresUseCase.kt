package com.example.mindlog.features.selfaware.domain.usecase

import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import javax.inject.Inject

class GetTopValueScoresUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(@Suppress("UNUSED_PARAMETER") unit: Unit = Unit) = repo.getTopValueScores()
}