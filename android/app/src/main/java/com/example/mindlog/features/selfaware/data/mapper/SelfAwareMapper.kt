package com.example.mindlog.features.selfaware.data.mapper

import com.example.mindlog.features.selfaware.data.dto.*
import com.example.mindlog.features.selfaware.domain.model.*
import java.time.Instant
import javax.inject.Inject

// feature/selfaware/data/mapper/SelfAwareMapper.kt
class SelfAwareMapper @Inject constructor() {

    fun parseInstant(s: String) = Instant.parse(s)

    fun toQuestion(dto: QuestionResponse) = Question(
        id = dto.id,
        type = dto.questionType,
        text = dto.text,
        categoriesKo = dto.categoriesKo,
        categoriesEn = dto.categoriesEn,
        createdAt = parseInstant(dto.createdAt)
    )

    fun toValueScore(dto: ValueScoreResponse) = ValueScore(
        category = dto.category,
        value = dto.value,
        confidence = dto.confidence,
        intensity = dto.intensity,
        polarity = dto.polarity,
        evidence = dto.evidenceQuotes
    )

    fun toAnswer(dto: AnswerResponse) = Answer(
        id = dto.id,
        questionId = dto.questionId,
        text = dto.text,
        createdAt = parseInstant(dto.createdAt),
        updatedAt = parseInstant(dto.updatedAt),
        valueScores = dto.valueScores.map(::toValueScore)
    )

    fun toTodayQA(dto: TodayQAResponse) = TodayQA(
        question = toQuestion(dto.question),
        answer = dto.answer?.let(::toAnswer)
    )

    fun toQAItem(dto: QAResponse) = QAItem(
        question = toQuestion(dto.question),
        answer = toAnswer(dto.answer)
    )
}