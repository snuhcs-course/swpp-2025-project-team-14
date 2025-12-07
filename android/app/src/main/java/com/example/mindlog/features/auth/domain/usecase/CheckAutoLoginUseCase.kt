package com.example.mindlog.core.domain.usecase

import com.example.mindlog.core.data.token.TokenManager
import com.example.mindlog.core.domain.Result
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class CheckAutoLoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) {
    sealed class Output {
        object GoToLogin : Output()
        object GoToTutorial : Output()
        object GoToHome : Output()
    }

    suspend operator fun invoke(hasCompletedTutorial: Boolean): Output {

        val access = tokenManager.getAccessToken()
        val refresh = tokenManager.getRefreshToken()

        if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
            return Output.GoToLogin
        }

        return if (tokenManager.isAccessTokenExpired()) {
            when (authRepository.refresh()) {
                is Result.Success -> decideNext(hasCompletedTutorial)
                is Result.Error -> {
                    tokenManager.clearTokens()
                    Output.GoToLogin
                }
            }
        } else {
            when (authRepository.verify()) {
                is Result.Success -> decideNext(hasCompletedTutorial)
                is Result.Error -> {
                    tokenManager.clearTokens()
                    Output.GoToLogin
                }
            }
        }
    }

    private fun decideNext(hasCompletedTutorial: Boolean): Output {
        return if (hasCompletedTutorial) Output.GoToHome
        else Output.GoToTutorial
    }
}