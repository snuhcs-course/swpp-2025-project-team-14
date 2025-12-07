package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class QAResponse(
    @SerializedName("question") val question: QuestionResponse,
    @SerializedName("answer") val answer: AnswerResponse? = null
)