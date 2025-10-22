package com.example.mindlog.selfaware

import app.cash.turbine.test
import com.example.mindlog.features.selfaware.domain.model.Answer
import com.example.mindlog.features.selfaware.domain.model.Question
import com.example.mindlog.features.selfaware.domain.model.TodayQA
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareViewModel
import com.example.mindlog.utils.MainDispatcherRule
import com.example.mindlog.utils.TestDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelfAwareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var getTodayQAUseCase: GetTodayQAUseCase
    private lateinit var submitAnswerUseCase: SubmitAnswerUseCase
    private lateinit var viewModel: SelfAwareViewModel

    @Before
    fun setup() {
        val testDispatcherProvider = TestDispatcherProvider(mainDispatcherRule.testDispatcher)

        getTodayQAUseCase = mock()
        submitAnswerUseCase = mock()
        viewModel = SelfAwareViewModel(getTodayQAUseCase, submitAnswerUseCase, testDispatcherProvider)
    }

    @Test fun `load success updates state`() = runTest {
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(
            com.example.mindlog.core.common.Result.Success(
                TodayQA(
                    question = Question(
                        1,
                        "A",
                        "당신은 안정적인 삶을 원하나요, 도전적인 삶을 원하나요?",
                        listOf("성장과 자기실현", "즐거움과 만족"),
                        listOf("Growth & Self-Actualization", "Enjoyment & Fulfillment"),
                        Instant.now(),
                    ),
                    answer = null
                )
            )
        )

        viewModel.state.test {
            awaitItem()

            viewModel.load()
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val done = awaitItem()
            assertFalse(done.isLoading)
            assertFalse(done.isSubmitting)
            assertFalse(done.isAnsweredToday)
            assertEquals(1, done.questionId)
            assertEquals("당신은 안정적인 삶을 원하나요, 도전적인 삶을 원하나요?", done.questionText)
            assertEquals(listOf("성장과 자기실현", "즐거움과 만족"), done.categoriesKo)

            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `load error sets error`() = runTest {
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(
            com.example.mindlog.core.common.Result.Error(message = "network")
        )

        viewModel.state.test {
            awaitItem()

            viewModel.load()
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val done = awaitItem()
            assertFalse(done.isLoading)
            assertFalse(done.isSubmitting)
            assertFalse(done.isAnsweredToday)
            assertEquals(null, done.questionId)
            assertEquals(null, done.questionText)
            assertEquals(null, done.categoriesKo)
            assertEquals("network", done.error)

            cancelAndConsumeRemainingEvents()
        }
    }


    @Test fun `submit with blank answer sets error`() = runTest {
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(
            com.example.mindlog.core.common.Result.Success(
                TodayQA(
                    question = Question(
                        1,
                        "A",
                        "당신은 안정적인 삶을 원하나요, 도전적인 삶을 원하나요?",
                        listOf("Growth & Self-Actualization", "Enjoyment & Fulfillment"),
                        listOf("성장과 자기실현", "즐거움과 만족"),
                        Instant.now(),
                    ),
                    answer = null
                )
            )
        )

        viewModel.state.test {
            awaitItem()

            viewModel.load()
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            viewModel.updateAnswerText("") // blank
            val blandAnswer = awaitItem()
            assertEquals("",blandAnswer.answerText)

            viewModel.submit()
            val notSubmitting = awaitItem()
            assertEquals("답변을 입력해주세요.", notSubmitting.error)

            cancelAndConsumeRemainingEvents()
        }
    }


    @Test
    fun `submit success marks answered`() = runTest {
        whenever(getTodayQAUseCase.invoke(any())).thenReturn(
            com.example.mindlog.core.common.Result.Success(
                TodayQA(
                    question = Question(
                        1,
                        "A",
                        "당신은 안정적인 삶을 원하나요, 도전적인 삶을 원하나요?",
                        listOf("Growth & Self-Actualization", "Enjoyment & Fulfillment"),
                        listOf("성장과 자기실현", "즐거움과 만족"),
                        Instant.now(),
                    ),
                    answer = null
                )
            )
        )
        whenever(
            submitAnswerUseCase.invoke(
                questionId = 1,
                answer = "저는 도전적이기 보다는 안정적인 삶을 원해요."
            )
        ).thenReturn(
            com.example.mindlog.core.common.Result.Success(
                Answer(
                    id = 1,
                    questionId = 1,
                    text = "저는 도전적이기 보다는 안정적인 삶을 원해요.",
                    Instant.now(),
                    Instant.now(),
                    null
                )
            )
        )

        viewModel.state.test {
            awaitItem() // initial

            viewModel.load()
            val loading = awaitItem()
            assertTrue(loading.isLoading)

            val loaded = awaitItem()
            assertFalse(loaded.isLoading)

            viewModel.updateAnswerText("저는 도전적이기 보다는 안정적인 삶을 원해요.")
            val updated = awaitItem()
            assertEquals("저는 도전적이기 보다는 안정적인 삶을 원해요.", updated.answerText)

            viewModel.submit()
            val submitting = awaitItem()
            assertTrue(submitting.isSubmitting)

            val done = awaitItem()
            assertFalse(done.isSubmitting)
            assertTrue(done.isAnsweredToday)

            cancelAndConsumeRemainingEvents()
        }

    }
}