package com.example.mindlog.features.analysis.data.dto

import com.google.gson.annotations.SerializedName

data class ComprehensiveAnalysisResponse(
    @SerializedName("conscientiousness") val conscientiousness: String,
    @SerializedName("neuroticism") val neuroticism: String,
    @SerializedName("extraversion") val extraversion: String,
    @SerializedName("openness") val openness: String,
    @SerializedName("agreeableness") val agreeableness: String,
    @SerializedName("updated_at") val updatedAt: String
)