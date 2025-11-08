package com.example.mindlog.features.statistics.data.api

import com.example.mindlog.features.statistics.data.dto.EmotionRatesResponse
import retrofit2.http.GET

interface StatisticsApi {
    @GET("/statistics/emotion-rate")
    suspend fun getEmotionRates(): EmotionRatesResponse
}