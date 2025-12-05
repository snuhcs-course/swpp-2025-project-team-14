package com.example.mindlog.features.statistics.data.dto

import com.google.gson.annotations.SerializedName

data class EmotionRatesResponse(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("statistics") val statistics: List<EmotionRateItem>
)