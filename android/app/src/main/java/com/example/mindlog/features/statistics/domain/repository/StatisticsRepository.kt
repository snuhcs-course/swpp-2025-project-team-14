package com.example.mindlog.features.statistics.domain.repository

import com.example.mindlog.core.domain.Result
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.JournalStatistics

interface StatisticsRepository {
    suspend fun getJournalStatisics(startDate: String, endDate: String): Result<JournalStatistics>
}