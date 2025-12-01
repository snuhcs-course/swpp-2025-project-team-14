package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class CategoryResponse(
    @SerializedName("category_en") val categoryEn: String,
    @SerializedName("category_ko") val categoryKo: String,
    @SerializedName("score") val score: Int
)