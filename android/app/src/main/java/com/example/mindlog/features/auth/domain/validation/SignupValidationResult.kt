package com.example.mindlog.features.auth.domain.validation

data class SignupValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)