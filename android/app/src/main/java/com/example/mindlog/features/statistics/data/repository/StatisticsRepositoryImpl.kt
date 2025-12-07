package com.example.mindlog.features.statistics.data.repository


import com.example.mindlog.core.domain.Result
import com.example.mindlog.core.domain.toResult
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StatisticsRepositoryImpl @Inject constructor(
    private val journalApi: JournalApi,
    private val mapper: StatisticsMapper,
    private val dispatcher: DispatcherProvider
): StatisticsRepository {
    override suspend fun getJournalStatisics(startDate: String, endDate: String): Result<JournalStatistics> = withContext(dispatcher.io) {
        runCatching {
            val journals = mutableListOf<JournalItemResponse>()
            var cursor: Int? = null

            do {
                val response = journalApi.searchJournals(
                    startDate = startDate,
                    endDate = endDate,
                    title = null,
                    limit = 50,
                    cursor = cursor,
                )

                journals += response.items
                cursor = response.nextCursor
            } while (cursor != null)
            mapper.toJournalStatistics(journals)
        }.toResult()
    }
}