package com.example.mindlog.features.statistics.domain.usecase

import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import javax.inject.Inject

class GetEmotionRatesUseCase @Inject constructor(private val repo: StatisticsRepository) {
    suspend operator fun invoke() = repo.getEmotionRates()
}

class GetJournalStatisticsUseCase @Inject constructor(private val repo: StatisticsRepository) {
    suspend operator fun invoke(startDate: String, endDate: String) =
        repo.getJournalStatisics(startDate, endDate)
}