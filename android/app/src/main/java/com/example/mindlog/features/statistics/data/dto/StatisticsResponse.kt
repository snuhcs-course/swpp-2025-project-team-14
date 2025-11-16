package com.example.mindlog.features.statistics.data.dto

import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend

data class StatisticsResponse(
    val emotionRatios: List<EmotionRatio>,
    val emotionTrends: List<EmotionTrend>
)