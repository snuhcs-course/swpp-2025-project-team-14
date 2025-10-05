package com.example.mindlog.features.auth.api

data class SignupRequest(
    val login_id: String,
    val password: String,
    val username: String
)

data class LoginRequest(
    val login_id: String,
    val password: String
)

data class RefreshTokenRequest(
    val refresh: String
)