package com.example.mindlog.features.statistics.data.mapper

import com.example.mindlog.features.statistics.data.dto.EmotionRateResponse
import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import javax.inject.Inject

class StatisticsMapper @Inject constructor() {
    fun toEmotionRate(dto: EmotionRateResponse) = EmotionRate(
        emotion =  dto.emotion,
        count = dto.count,
        percentage = dto.percentage
    )
}