package com.example.mindlog.features.statistics.data.dto

import com.google.gson.annotations.SerializedName


data class EmotionRateItem(
    @SerializedName("emotion") val emotion: String,
    @SerializedName("count") val count: Int,
    @SerializedName("percentage") val percentage: Float
)