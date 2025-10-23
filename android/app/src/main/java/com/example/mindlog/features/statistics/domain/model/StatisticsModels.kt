package com.example.mindlog.features.statistics.domain.model

data class EmotionRatio(
    val emotion: String,
    val value: Float
)

data class EmotionTrend(
    val emotion: String,
    val trend: List<Float>
)