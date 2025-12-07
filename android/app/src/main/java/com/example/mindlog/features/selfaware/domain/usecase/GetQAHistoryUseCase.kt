package com.example.mindlog.features.selfaware.domain.usecase

import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import javax.inject.Inject

class GetQAHistoryUseCase @Inject constructor(private val repo: SelfAwareRepository) {
    suspend operator fun invoke(limit: Int, cursor: Int?) = repo.getQAHistory(limit, cursor)
}