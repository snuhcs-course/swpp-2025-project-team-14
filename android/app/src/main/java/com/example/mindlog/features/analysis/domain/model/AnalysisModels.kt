package com.example.mindlog.features.analysis.domain.model

data class UserType(
    val userType: String,
    val updatedAt: String
)

data class ComprehensiveAnalysis(
    val text: String,
    val updatedAt: String
)

data class PersonalizedAdvice(
    val adviceType: String,
    val text: String,
    val updatedAt: String
)