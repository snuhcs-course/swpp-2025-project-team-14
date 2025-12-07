package com.example.mindlog.features.auth.presentation.main

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.mindlog.core.domain.usecase.CheckAutoLoginUseCase
import com.example.mindlog.testutil.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var useCase: CheckAutoLoginUseCase
    private lateinit var viewModel: MainViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        useCase = mock(CheckAutoLoginUseCase::class.java)
        viewModel = MainViewModel(useCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GoToLogin output from usecase sets state to GoLogin`() = runTest {
        // given
        `when`(useCase.invoke(false)).thenReturn(CheckAutoLoginUseCase.Output.GoToLogin)

        // when
        viewModel.checkAutoLogin(false)
        advanceUntilIdle()

        // then
        val state = viewModel.state.getOrAwaitValue()
        assertEquals(MainState.GoLogin, state)
    }

    @Test
    fun `GoToHome output from usecase sets state to GoHome`() = runTest {
        // given
        `when`(useCase.invoke(true)).thenReturn(CheckAutoLoginUseCase.Output.GoToHome)

        // when
        viewModel.checkAutoLogin(true)
        advanceUntilIdle()

        // then
        val state = viewModel.state.getOrAwaitValue()
        assertEquals(MainState.GoHome, state)
    }

    @Test
    fun `GoToTutorial output from usecase sets state to GoTutorial`() = runTest {
        // given
        `when`(useCase.invoke(false)).thenReturn(CheckAutoLoginUseCase.Output.GoToTutorial)

        // when
        viewModel.checkAutoLogin(false)
        advanceUntilIdle()

        // then
        val state = viewModel.state.getOrAwaitValue()
        assertEquals(MainState.GoTutorial, state)
    }
}