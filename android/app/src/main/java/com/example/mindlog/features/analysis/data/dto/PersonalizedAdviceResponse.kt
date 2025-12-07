package com.example.mindlog.features.analysis.data.dto

import com.google.gson.annotations.SerializedName


data class PersonalizedAdviceResponse(
    @SerializedName("advice_type") val adviceType: String,
    @SerializedName("personalized_advice") val personalizedAdvice: String,
    @SerializedName("updated_at") val updatedAt: String
)

