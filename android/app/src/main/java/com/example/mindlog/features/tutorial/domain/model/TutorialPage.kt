package com.example.mindlog.features.tutorial.domain.model

import androidx.annotation.DrawableRes

data class TutorialPage(
    @DrawableRes val imageRes: Int,
    val feature: String,
)