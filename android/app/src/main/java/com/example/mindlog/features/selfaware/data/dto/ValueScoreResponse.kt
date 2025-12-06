package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class ValueScoreResponse(
    @SerializedName("value")     val value: String,
    @SerializedName("intensity") val intensity: Float
)