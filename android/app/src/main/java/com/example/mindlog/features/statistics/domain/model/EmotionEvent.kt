package com.example.mindlog.features.statistics.domain.model

data class EmotionEvent(
    val emotion: Emotion,
    val events: List<String>
)