package com.example.mindlog.features.auth.api

data class TokenResponse(
    val access: String,
    val refresh: String
)

data class TokenResponseEnvelope(
    val data: TokenResponse
)