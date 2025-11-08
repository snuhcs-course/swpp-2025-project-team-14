package com.example.mindlog.features.statistics.domain.respository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.domain.model.EmotionRate

interface StatisticsRepository {
    suspend fun getEmotionRates(): Result<List<EmotionRate>>
}