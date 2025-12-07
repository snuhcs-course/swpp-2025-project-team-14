package com.example.mindlog.features.statistics.domain.model

fun toKo(emotion: Emotion?): String? = when (emotion) {
    Emotion.HAPPY -> "행복"
    Emotion.SAD -> "슬픔"
    Emotion.ANXIOUS -> "불안"
    Emotion.CALM -> "평안"
    Emotion.ANNOYED -> "짜증"
    Emotion.SATISFIED -> "만족"
    Emotion.BORED -> "지루함"
    Emotion.INTERESTED -> "흥미"
    Emotion.LETHARGIC -> "무기력"
    Emotion.ENERGETIC -> "활력"
    null -> null
}