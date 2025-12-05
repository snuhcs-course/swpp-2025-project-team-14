package com.example.mindlog.features.selfaware.domain.model

import java.time.LocalDate

data class Question(
    val id: Int,
    val type: String,
    val text: String,
    val createdAt: LocalDate
)