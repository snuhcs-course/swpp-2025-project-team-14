package com.example.mindlog.features.auth.domain.model

data class Token(
    val accessToken: String,
    val refreshToken: String
)