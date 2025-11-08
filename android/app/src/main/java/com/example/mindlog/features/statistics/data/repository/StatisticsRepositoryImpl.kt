package com.example.mindlog.features.statistics.data.repository


import com.example.mindlog.core.common.toResult
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.statistics.data.api.StatisticsApi
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val statisticsApi: StatisticsApi,
    private val mapper: StatisticsMapper,
    private val dispatcher: DispatcherProvider
): StatisticsRepository {

    override suspend fun getEmotionRates() = withContext(dispatcher.io) {
        runCatching {
            val dto = statisticsApi.getEmotionRates()
            val emotionRates = dto.emotionRates.map(mapper::toEmotionRate)
            emotionRates.toList()
        }.toResult()
    }
}