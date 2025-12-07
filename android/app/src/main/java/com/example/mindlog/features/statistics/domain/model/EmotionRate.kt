package com.example.mindlog.features.statistics.domain.model

data class EmotionRate(
    val emotion: Emotion,
    val count: Int,
    val percentage: Float
)