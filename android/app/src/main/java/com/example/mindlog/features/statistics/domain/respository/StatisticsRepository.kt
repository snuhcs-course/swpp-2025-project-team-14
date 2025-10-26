package com.example.mindlog.features.statistics.domain.respository

import com.example.mindlog.core.common.Result
import com.example.mindlog.features.statistics.data.dto.StatisticsResponse

interface StatisticsRepository {
    suspend fun getStatistics(): Result<StatisticsResponse>
}