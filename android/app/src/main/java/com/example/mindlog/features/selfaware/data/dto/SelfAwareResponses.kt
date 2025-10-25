package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName
import java.util.Dictionary


data class QuestionResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("question_type")  val questionType: String,
    @SerializedName("text")           val text: String,
    @SerializedName("categories_ko")  val categoriesKo: List<String>?,
    @SerializedName("categories_en")  val categoriesEn: List<String>?,
    @SerializedName("created_at")     val createdAt: String // ISO8601
)

data class AnswerResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("type")        val type: String,
    @SerializedName("text")        val text: String,
    @SerializedName("created_at")  val createdAt: String, // ISO8601
    @SerializedName("updated_at")  val updatedAt: String // ISO8601
)

data class QAResponse(
    @SerializedName("question") val question: QuestionResponse,
    @SerializedName("Answer") val answer: AnswerResponse? = null
)

data class QACursorResponse(
    @SerializedName("items") val items: List<QAResponse>,
    @SerializedName("next_cursor") val next_cursor: Int
)

data class ValueScoreResponse(
    @SerializedName("value_scores") val valueScores: Dictionary<String, Any>,
)

data class ValueMapResponse(
    @SerializedName("category_scores") val categoryScores: Dictionary<String, Any>,
    @SerializedName("updated_at") val updatedAt: String // ISO8601
)

data class PersonalityInsightResponse(
    @SerializedName("comment")              val comment: String,
    @SerializedName("personality_insight")  val personalityInsight: String,
    @SerializedName("updated_at")           val updatedAt: String
)