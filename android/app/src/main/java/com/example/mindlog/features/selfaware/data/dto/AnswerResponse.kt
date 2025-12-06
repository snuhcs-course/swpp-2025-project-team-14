package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class AnswerResponse(
    @SerializedName("id")          val id: Int,
    @SerializedName("question_id") val questionId: Int,
    @SerializedName("type")        val type: String,
    @SerializedName("text")        val text: String,
    @SerializedName("created_at")  val createdAt: String, // ISO8601
    @SerializedName("updated_at")  val updatedAt: String // ISO8601
)