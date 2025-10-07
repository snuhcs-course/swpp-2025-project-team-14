package com.example.mindlog.features.auth.data.api

import com.google.gson.annotations.SerializedName

data class SignupRequest(
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String,
    @SerializedName("username") val username: String
)

data class LoginRequest(
    @SerializedName("loginId") val loginId: String,
    @SerializedName("password") val password: String
)

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)
