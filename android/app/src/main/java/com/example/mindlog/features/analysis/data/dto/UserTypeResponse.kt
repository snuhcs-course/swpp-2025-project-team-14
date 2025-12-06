package com.example.mindlog.features.analysis.data.dto

import com.google.gson.annotations.SerializedName

data class UserTypeResponse(
    @SerializedName("user_type") val userType: String,
    @SerializedName("description") val description: String,
    @SerializedName("updated_at") val updatedAt: String
)