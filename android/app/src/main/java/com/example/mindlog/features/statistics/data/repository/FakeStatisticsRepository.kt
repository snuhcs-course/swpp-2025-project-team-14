package com.example.mindlog.features.statistics.data.repository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.data.dto.StatisticsResponse
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import javax.inject.Inject

// features/statistics/data/fake/FakeStatisticsRepository.kt
class FakeStatisticsRepository @Inject constructor() : StatisticsRepository {

    override suspend fun getStatistics(): Result<StatisticsResponse> {
        // 데모 데이터
        val ratios = listOf(
            EmotionRatio("기쁨", 0.42f),
            EmotionRatio("불안", 0.18f),
            EmotionRatio("슬픔", 0.12f),
            EmotionRatio("피곤", 0.28f)
        )

        val trends = listOf(
            EmotionTrend("기쁨", listOf(0.30f, 0.45f, 0.50f, 0.55f, 0.62f, 0.58f, 0.66f)),
            EmotionTrend("불안", listOf(0.40f, 0.35f, 0.25f, 0.30f, 0.22f, 0.20f, 0.18f)),
            EmotionTrend("슬픔", listOf(0.20f, 0.15f, 0.10f, 0.12f, 0.08f, 0.09f, 0.07f)),
            EmotionTrend("피곤", listOf(0.50f, 0.48f, 0.42f, 0.40f, 0.36f, 0.38f, 0.33f))
        )

        return Result.Success(StatisticsResponse(ratios, trends))
    }
}