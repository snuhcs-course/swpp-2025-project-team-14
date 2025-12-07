package com.example.mindlog.features.statistics.domain.model

data class JournalStatistics(
    val EmotionTrends: List<EmotionTrend>,
    val EmotionEvents: List<EmotionEvent>,
    val JournalKeywords: List<JournalKeyword>
)