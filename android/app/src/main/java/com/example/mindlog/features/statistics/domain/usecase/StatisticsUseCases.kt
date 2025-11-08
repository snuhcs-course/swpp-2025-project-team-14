package com.example.mindlog.features.statistics.domain.usecase

import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import javax.inject.Inject

class GetEmotionRates @Inject constructor(private val repo: StatisticsRepository) {
    suspend operator fun invoke() = repo.getEmotionRates()

}