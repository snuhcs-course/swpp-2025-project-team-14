package com.example.mindlog.features.auth.data.api

data class TokenResponseEnvelope(
    val data: TokenData
)

data class TokenData(
    val access: String,
    val refresh: String
)