package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class QuestionResponse(
    @SerializedName("id")             val id: Int,
    @SerializedName("question_type")  val questionType: String,
    @SerializedName("text")           val text: String,
    @SerializedName("created_at")     val createdAt: String // ISO8601
)