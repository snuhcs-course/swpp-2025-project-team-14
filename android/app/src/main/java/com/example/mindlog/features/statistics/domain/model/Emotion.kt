package com.example.mindlog.features.statistics.domain.model

enum class Emotion(val apiValue: String) {
    HAPPY("happy"),
    SAD("sad"),
    ANXIOUS("anxious"),
    CALM("calm"),
    ANNOYED("annoyed"),
    SATISFIED("satisfied"),
    BORED("bored"),
    INTERESTED("interested"),
    LETHARGIC("lethargic"),
    ENERGETIC("energetic");

    companion object {
        fun fromApi(value: String): Emotion? =
            entries.firstOrNull { it.apiValue.equals(value, ignoreCase = true) }
    }
}