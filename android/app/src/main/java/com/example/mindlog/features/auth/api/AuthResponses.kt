package com.example.mindlog.features.auth.api.model

// 서버에서 내려주는 TokenResponse
data class TokenResponse(
    val access: String,
    val refresh: String
)

// Envelope 형태
data class TokenResponseEnvelope(
    val success: Boolean,
    val message: String?,
    val data: TokenResponse?
)