package com.example.mindlog.features.selfaware.domain.model

import java.time.LocalDate

data class ValueMap(
    val categoryScores: List<CategoryScore>,
    val updatedAt: LocalDate
)