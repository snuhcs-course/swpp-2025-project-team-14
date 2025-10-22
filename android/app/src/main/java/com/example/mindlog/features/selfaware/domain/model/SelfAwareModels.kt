package com.example.mindlog.features.selfaware.domain.model

import java.time.Instant

data class TodayQA(
    val question: Question,
    val answer: Answer?
)

data class Question(
    val id: Int,
    val type: String,
    val text: String,
    val categoriesKo: List<String>?,
    val categoriesEn: List<String>?,
    val createdAt: Instant
)

data class Answer(
    val id: Int,
    val questionId: Int,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val valueScores: List<ValueScore>?
)

data class ValueScore(
    val category: String,
    val value: String,
    val confidence: Float,
    val intensity: Float,
    val polarity: Int,
    val evidence: List<String>?
)

data class Today(
    val question: Question,
    val answer: Answer?
)

data class QAItem(
    val question: Question,
    val answer: Answer
)
