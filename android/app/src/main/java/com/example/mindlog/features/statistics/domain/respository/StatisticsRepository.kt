package com.example.mindlog.features.statistics.domain.respository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.data.dto.StatisticsResponse
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend

interface StatisticsRepository {
    suspend fun getEmotionRatio(): Result<List<EmotionRatio>>
    suspend fun getEmotionTrend(): Result<List<EmotionTrend>>
    suspend fun getEmotionEvents(emotion: String): Result<List<String>>
    suspend fun getJournalKeywords(): Result<List<String>>
}