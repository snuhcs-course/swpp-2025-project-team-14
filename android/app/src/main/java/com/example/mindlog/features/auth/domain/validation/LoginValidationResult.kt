package com.example.mindlog.features.auth.domain.validation

data class LoginValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)