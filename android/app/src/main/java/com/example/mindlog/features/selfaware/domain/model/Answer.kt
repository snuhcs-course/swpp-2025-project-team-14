package com.example.mindlog.features.selfaware.domain.model

import java.time.LocalDate

data class Answer(
    val id: Int,
    val questionId: Int,
    val type: String?,
    val text: String,
    val createdAt: LocalDate,
    val updatedAt: LocalDate,
)