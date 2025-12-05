package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class TopValueScoresResponse(
    @SerializedName("value_scores") val valueScores: List<ValueScoreResponse>,
)