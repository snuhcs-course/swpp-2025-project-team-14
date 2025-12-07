package com.example.mindlog.statistics

import com.example.mindlog.core.domain.Result
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.data.dto.JournalListResponse
import com.example.mindlog.features.statistics.data.mapper.StatisticsMapper
import com.example.mindlog.features.statistics.data.repository.StatisticsRepositoryImpl
import com.example.mindlog.features.statistics.domain.model.JournalStatistics
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var journalApi: JournalApi
    private lateinit var mapper: StatisticsMapper
    private lateinit var repo: StatisticsRepositoryImpl

    private lateinit var dispatcherProvider: TestDispatcherProvider

    @Before
    fun setUp() {
        journalApi = mock()
        mapper = mock()
        dispatcherProvider =  TestDispatcherProvider(mainDispatcherRule.testDispatcher)
        repo = StatisticsRepositoryImpl(journalApi, mapper, dispatcherProvider)
    }

    @Test
    fun `getJournalStatisics returns Success when mapper succeeds`() = runTest{
        // given: one-page paging to avoid depending on response concrete class
        // Use real dummy response and items.
        val items = listOf(
            JournalItemResponse(
                id = 1,
                title = "테스트 일기 1",
                content = "오늘은 테스트 데이터를 작성했다.",
                emotions = emptyList(),
                gratitude = "감사합니다",
                imageS3Keys = null,
                createdAt = "2025-11-01T10:00:00Z",
                keywords = emptyList()
            ),
            JournalItemResponse(
                id = 2,
                title = "테스트 일기 2",
                content = "테스트 중 두 번째 일기입니다.",
                emotions = emptyList(),
                gratitude = "소중한 하루",
                imageS3Keys = null,
                createdAt = "2025-11-02T10:00:00Z",
                keywords = emptyList()
            )
        )
        val fakeResponse = JournalListResponse(
            items = items,
            nextCursor = null
        )
        whenever(
            journalApi.searchJournals(
                startDate = eq("2025-11-01"),
                endDate = eq("2025-11-07"),
                title = isNull(),
                limit = eq(50),
                cursor = isNull()
            )
        ).thenReturn(fakeResponse)

        val mapped = JournalStatistics(
            EmotionRates = emptyList(),
            EmotionTrends = emptyList(),
            EmotionEvents = emptyList(),
            JournalKeywords = emptyList()
        )
        whenever(mapper.toJournalStatistics(items)).thenReturn(mapped)

        // when
        val res = repo.getJournalStatisics("2025-11-01", "2025-11-07")
        print(res)

        // then
        verify(journalApi).searchJournals(
            startDate = eq("2025-11-01"),
            endDate = eq("2025-11-07"),
            title = eq(null),
            limit = eq(50),
            cursor = eq(null)
        )
        verify(mapper).toJournalStatistics(eq(items))
        assertEquals(Result.Success(mapped), res)
    }

    @Test
    fun `getJournalStatisics returns Error when api throws`() = runTest {
        whenever(
            journalApi.searchJournals(
                any(),
                any(),
                isNull(),
                any(),
                isNull()
            )
        ).thenThrow(IllegalStateException("journal api down"))

        val res = repo.getJournalStatisics("2025-11-01", "2025-11-07")

        assertTrue(res is Result.Error)
        assertTrue((res as Result.Error).message?.contains("journal api down") == true)
    }
}