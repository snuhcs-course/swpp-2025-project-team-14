package com.example.mindlog.features.statistics.domain.model

data class EmotionTrend(
    val emotion: Emotion,
    val trend: List<Int>
)