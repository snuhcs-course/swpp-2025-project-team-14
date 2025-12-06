package com.example.mindlog.features.selfaware.data.dto

import com.google.gson.annotations.SerializedName

data class QACursorResponse(
    @SerializedName("items") val items: List<QAResponse>,
    @SerializedName("next_cursor") val next_cursor: Int
)