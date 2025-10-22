package com.example.mindlog.selfaware

import com.example.mindlog.core.common.Paged
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.usecase.GetHistoryUseCase
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareHistoryViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelfAwareHistoryViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var getHistoryUseCase: GetHistoryUseCase
    private lateinit var viewModel: SelfAwareHistoryViewModel

    @Before fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getHistoryUseCase = mock()
        viewModel = SelfAwareHistoryViewModel(getHistoryUseCase, testDispatcherProvider)
    }

    @Test
    fun `refresh sets isEnd true when fewer than requested returned`() = runTest {
        val page1 = Paged(items = listOf(q(1), q(2)), cursor = 1, size = 20)
        whenever(getHistoryUseCase.invoke(cursor = eq(1), size = any()))
            .thenReturn(Result.Success(page1))

        viewModel.refresh()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(1, s.cursor)
        assertEquals(2, s.items.size)
        assertTrue(s.isEnd)
        assertFalse(s.isLoading)
    }

    @Test
    fun `refresh sets isEnd false when full page returned`() = runTest {
        val full = (1..20).map { q(it) }
        val page1 = Paged(items = full, cursor = 1, size = 20)
        whenever(getHistoryUseCase.invoke(cursor = eq(1), size = any()))
            .thenReturn(Result.Success(page1))

        viewModel.refresh()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(20, s.items.size)
        assertFalse(s.isEnd)
    }

    @Test
    fun `loadNext appends more items and marks end when second page is short`() = runTest {
        val pageSize = 20
        val page1 = Paged(items = (1..pageSize).map { q(it) }, cursor = 1, size = pageSize)
        val page2 = Paged(items = (21..25).map { q(it) }, cursor = 2, size = pageSize) // 5개만

        whenever(getHistoryUseCase.invoke(cursor = eq(1), size = any()))
            .thenReturn(Result.Success(page1))
        whenever(getHistoryUseCase.invoke(cursor = eq(2), size = any()))
            .thenReturn(Result.Success(page2))

        viewModel.refresh()
        advanceUntilIdle()

        viewModel.loadNext()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals(2, s.cursor)
        assertEquals(25, s.items.size)
        assertTrue(s.isEnd)
        assertFalse(s.isLoading)
    }

    @Test
    fun `refresh error populates error and stops loading`() = runTest {
        whenever(getHistoryUseCase.invoke(cursor = eq(1), size = any()))
            .thenReturn(Result.Error(message = "network_error"))

        viewModel.refresh()
        advanceUntilIdle()

        val s = viewModel.state.value
        assertEquals("network_error", s.error)
        assertFalse(s.isLoading)
        assertTrue(s.items.isEmpty())
    }


    private fun q(id: Int) = QAItem(
        question = Question(
            id = id,
            type = "Question Type",
            text = "Q$id",
            categoriesEn = listOf("Growth & Self-Actualization", "Enjoyment & Fulfillment"),
            categoriesKo = listOf("성장과 자기실현", "즐거움과 만족"),
            createdAt = Instant.now(),
        ),
        answer = Answer(
            id = id,
            questionId = id,
            text = "A$id",
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            valueScores = null
        )
    )
}