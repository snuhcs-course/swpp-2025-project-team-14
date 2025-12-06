package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName


data class ValueMapResponse(
    @SerializedName("category_scores") val categoryScores: List<CategoryResponse>,
    @SerializedName("updated_at") val updatedAt: String // ISO8601
)
