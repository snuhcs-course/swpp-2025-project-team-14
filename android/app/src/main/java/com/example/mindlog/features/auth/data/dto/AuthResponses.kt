package com.example.mindlog.features.auth.data.dto

import com.google.gson.annotations.SerializedName

data class TokenResponse(
    @SerializedName("access") val access: String,
    @SerializedName("refresh") val refresh: String
)

data class LogoutData(
    @SerializedName("status") val status: String
)

data class LogoutResponse(
    @SerializedName("ok") val ok: Boolean,
    @SerializedName("data") val data: LogoutData?,
    @SerializedName("error") val error: String?
)