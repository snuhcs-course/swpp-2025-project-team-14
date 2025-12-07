package com.example.mindlog.auth

import com.example.mindlog.core.data.token.TokenManager
import com.example.mindlog.core.domain.Result
import com.example.mindlog.core.domain.usecase.CheckAutoLoginUseCase
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import com.example.mindlog.features.auth.domain.usecase.RefreshTokenUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import java.time.LocalDate

class AuthUseCasesTest {

    private lateinit var repo: AuthRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var loginUseCase: LoginUseCase
    private lateinit var signupUseCase: SignupUseCase
    private lateinit var refreshUseCase: RefreshTokenUseCase
    private lateinit var checkAutoLoginUseCase: CheckAutoLoginUseCase


    @Before
    fun setUp() {
        repo = mock(AuthRepository::class.java)
        tokenManager = mock(TokenManager::class.java)
        loginUseCase = LoginUseCase(repo)
        signupUseCase = SignupUseCase(repo)
        refreshUseCase = RefreshTokenUseCase(repo)
        checkAutoLoginUseCase = CheckAutoLoginUseCase(repo, tokenManager)
    }

    @Test
    fun `loginUseCase calls repo and returns success`() = runBlocking {
        // given
        `when`(repo.login("id", "pw")).thenReturn(Result.Success(true))

        // when
        val result = loginUseCase("id", "pw")

        // then
        verify(repo).login("id", "pw")
        assertTrue(result is Result.Success && result.data)
    }

    @Test
    fun `signupUseCase calls repo and returns success`() = runBlocking {
        // given
        val date = LocalDate.parse("2000-01-01")
        `when`(repo.signup("id", "pw", "user", "M", date))
            .thenReturn(Result.Success(true))

        // when
        val result = signupUseCase("id", "pw", "user", "M", date)

        // then
        verify(repo).signup("id", "pw", "user", "M", date)
        assertTrue(result is Result.Success && result.data)
    }

    @Test
    fun `refreshUseCase calls repo and returns success`() = runBlocking {
        // given
        `when`(repo.refresh()).thenReturn(Result.Success(true))

        // when
        val result = refreshUseCase()

        // then
        verify(repo).refresh()
        assertTrue(result is Result.Success && result.data)
    }

    @Test
    fun `no tokens returns GoToLogin and does not call repo`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn(null)
        `when`(tokenManager.getRefreshToken()).thenReturn(null)

        // when
        val output = checkAutoLoginUseCase(hasCompletedTutorial = true)

        // then
        assertTrue(output is CheckAutoLoginUseCase.Output.GoToLogin)
        verifyNoInteractions(repo)
    }

    @Test
    fun `expired token with successful refresh and completed tutorial goes to home`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AA")
        `when`(tokenManager.getRefreshToken()).thenReturn("RR")
        `when`(tokenManager.isAccessTokenExpired()).thenReturn(true)
        `when`(repo.refresh()).thenReturn(Result.Success(true))

        // when
        val output = checkAutoLoginUseCase(hasCompletedTutorial = true)

        // then
        assertTrue(output is CheckAutoLoginUseCase.Output.GoToHome)
        verify(repo).refresh()
        verify(tokenManager, never()).clearTokens()
    }

    @Test
    fun `expired token with refresh error clears tokens and goes to login`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AA")
        `when`(tokenManager.getRefreshToken()).thenReturn("RR")
        `when`(tokenManager.isAccessTokenExpired()).thenReturn(true)
        `when`(repo.refresh()).thenReturn(Result.Error(code = 401, message = "expired"))

        // when
        val output = checkAutoLoginUseCase(hasCompletedTutorial = false)

        // then
        assertTrue(output is CheckAutoLoginUseCase.Output.GoToLogin)
        verify(repo).refresh()
        verify(tokenManager).clearTokens()
    }

    @Test
    fun `valid token with successful verify and not completed tutorial goes to tutorial`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AA")
        `when`(tokenManager.getRefreshToken()).thenReturn("RR")
        `when`(tokenManager.isAccessTokenExpired()).thenReturn(false)
        `when`(repo.verify()).thenReturn(Result.Success(true))

        // when
        val output = checkAutoLoginUseCase(hasCompletedTutorial = false)

        // then
        assertTrue(output is CheckAutoLoginUseCase.Output.GoToTutorial)
        verify(repo).verify()
        verify(tokenManager, never()).clearTokens()
    }

    @Test
    fun `valid token with verify error clears tokens and goes to login`() = runTest {
        // given
        `when`(tokenManager.getAccessToken()).thenReturn("AA")
        `when`(tokenManager.getRefreshToken()).thenReturn("RR")
        `when`(tokenManager.isAccessTokenExpired()).thenReturn(false)
        `when`(repo.verify()).thenReturn(Result.Error(code = 401, message = "invalid"))

        // when
        val output = checkAutoLoginUseCase(hasCompletedTutorial = true)

        // then
        assertTrue(output is CheckAutoLoginUseCase.Output.GoToLogin)
        verify(repo).verify()
        verify(tokenManager).clearTokens()
    }
}