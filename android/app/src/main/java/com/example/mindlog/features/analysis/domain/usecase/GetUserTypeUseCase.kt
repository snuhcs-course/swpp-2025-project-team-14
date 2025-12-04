package com.example.mindlog.features.analysis.domain.usecase

import com.example.mindlog.core.domain.Result
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.analysis.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetUserTypeUseCase @Inject constructor(private val repo: AnalysisRepository) {
    suspend operator fun invoke(): Result<UserType> = repo.getUserType()
}