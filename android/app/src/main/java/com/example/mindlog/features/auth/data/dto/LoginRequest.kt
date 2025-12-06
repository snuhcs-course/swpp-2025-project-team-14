package com.example.mindlog.features.auth.data.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("login_id") val loginId: String,
    @SerializedName("password") val password: String
)