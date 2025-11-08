package com.example.mindlog.features.statistics.domain.model

data class EmotionRate(
    val emotion: String,
    val count: Int,
    val percentage: Float
)

data class JournalStatistics(
    val EmotionTrends: List<EmotionTrend>,
    val EmotionEvents: List<EmotionEvent>,
    val JournalKeywords: List<JournalKeyword>
)

data class EmotionTrend(
    val emotion: String,
    val trend: List<Int>
)

data class EmotionEvent(
    val emotion: String,
    val events: List<String>
)

data class JournalKeyword(
    val keyword: String,
    val count: Int
)