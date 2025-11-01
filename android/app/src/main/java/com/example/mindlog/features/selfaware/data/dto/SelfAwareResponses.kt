package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName


data class TodayQAResponse(
    @SerializedName("question") val question: QuestionResponse,
    @SerializedName("answer") val answer: AnswerResponse? = null
)

data class QuestionResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("user_id")        val userId: Int,
    @SerializedName("question_type")  val questionType: String,
    @SerializedName("text")           val text: String,
    @SerializedName("categories_ko")  val categoriesKo: List<String>?,
    @SerializedName("categories_en")  val categoriesEn: List<String>?,
    @SerializedName("created_at")     val createdAt: String // ISO8601
)

data class AnswerResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("user_id")     val userId: Int,
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("text")        val text: String,
    @SerializedName("created_at")  val createdAt: String,
    @SerializedName("updated_at")  val updatedAt: String,
    @SerializedName("value_scores") val valueScores: List<ValueScoreResponse> = emptyList()
)

data class ValueScoreResponse(
    @SerializedName("id")              val id: Int,
    @SerializedName("answer_id")       val answerId: Int,
    @SerializedName("user_id")         val userId: Int,
    @SerializedName("category")        val category: String,
    @SerializedName("value")           val value: String,
    @SerializedName("confidence")      val confidence: Float, // 0..1
    @SerializedName("intensity")       val intensity: Float,  // 0..1
    @SerializedName("polarity")        val polarity: Int,     // -1/0/+1
    @SerializedName("evidence_quotes") val evidenceQuotes: List<String>?,
    @SerializedName("created_at")      val createdAt: String
)

data class QAHistoryResponse(
    @SerializedName("items") val items: List<QAResponse>,
    @SerializedName("cursor") val cursor: Int,
    @SerializedName("size")  val size: Int,
    @SerializedName("total") val total: Int
)

data class QAResponse(
    @SerializedName("question") val question: QuestionResponse,
    @SerializedName("Answer") val answer: AnswerResponse
)