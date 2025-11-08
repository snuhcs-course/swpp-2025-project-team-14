package com.example.mindlog.features.statistics.domain.model

data class EmotionRate(
    val emotion: String,
    val count: Int,
    val percentage: Float
)
