package com.example.mindlog.features.settings.data.dto

import com.google.gson.annotations.SerializedName

data class PasswordUpdateRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String
)
