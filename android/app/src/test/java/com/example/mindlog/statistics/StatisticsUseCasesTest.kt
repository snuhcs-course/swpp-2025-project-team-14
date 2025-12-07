package com.example.mindlog.statistics

import com.example.mindlog.core.domain.Result
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import com.example.mindlog.features.statistics.domain.usecase.GetJournalStatisticsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsUseCasesTest {

    private lateinit var repo: StatisticsRepository
    private lateinit var getJournalStatistics: GetJournalStatisticsUseCase

    @Before
    fun setUp() {
        repo = mock()
        getJournalStatistics = GetJournalStatisticsUseCase(repo)
    }

    @Test
    fun `GetJournalStatisticsUseCase delegates to repository and returns success`() = runTest {
        // given
        val start = "2025-11-01"
        val end = "2025-11-07"
        val stats = JournalStatistics(
            EmotionRates = emptyList(),
            EmotionTrends = listOf(EmotionTrend(Emotion.CALM, listOf(1, 2, 3))),
            EmotionEvents = emptyList(),
            JournalKeywords = listOf(JournalKeyword("운동", 3))
        )
        whenever(repo.getJournalStatisics(eq(start), eq(end))).thenReturn(Result.Success(stats))

        // when
        val res = getJournalStatistics(start, end)

        // then
        verify(repo).getJournalStatisics(eq(start), eq(end))
        assertEquals(Result.Success(stats), res)
    }

    @Test
    fun `GetJournalStatisticsUseCase propagates repository error`() = runTest {
        // given
        val start = "2025-11-01"
        val end = "2025-11-07"
        whenever(repo.getJournalStatisics(any(), any())).thenReturn(Result.Error(500,"server error"))

        // when
        val res = getJournalStatistics(start, end)

        // then
        verify(repo).getJournalStatisics(eq(start), eq(end))
        assertEquals("server error", (res as Result.Error).message)
    }
}