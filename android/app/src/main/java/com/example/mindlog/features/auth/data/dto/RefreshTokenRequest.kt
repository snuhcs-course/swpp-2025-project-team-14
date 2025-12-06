package com.example.mindlog.features.auth.data.dto

import com.google.gson.annotations.SerializedName

data class RefreshTokenRequest(
    @SerializedName("refresh") val refreshToken: String
)
