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

    fun toAnswer(dto: AnswerResponse) = Answer(
        id = dto.id,
        questionId = dto.questionId,
        type = dto.type,
        text = dto.text,
        createdAt = parseInstant(dto.createdAt),
        updatedAt = parseInstant(dto.updatedAt),
    )

    fun toQAItem(dto: QAResponse) = QAItem(
        question = toQuestion(dto.question),
        answer = dto.answer?.let(::toAnswer)
    )

    private fun toValueScore(dto: ValueScoreResponse): ValueScore {
        return ValueScore(
            value = dto.value,
            intensity = dto.intensity
        )
    }

    private fun toCategory(dto: CategoryResponse): CategoryScore {
        return CategoryScore(
            categoryEn = dto.categoryEn,
            categoryKo = dto.categoryKo,
            score = dto.score
        )
    }

    fun toValueMap(dto: ValueMapResponse): ValueMap {
        return ValueMap(
            categoryScores = dto.categoryScores.map(::toCategory),
            updatedAt = parseInstant(dto.updatedAt)
        )
    }

    fun toPersonalityInsight(dto: PersonalityInsightResponse): PersonalityInsight {
        return PersonalityInsight(
            comment = dto.comment,
            personalityInsight = dto.personalityInsight,
            updatedAt = parseInstant(dto.updatedAt)
        )
    }
}