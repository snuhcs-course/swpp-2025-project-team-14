package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.domain.model.Token

class SignupUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(loginId: String, password: String, username: String): Result<Token> {
        return try {
            val token = repository.signup(loginId, password, username)
            if (token != null) {
                Result.success(token)
            } else {
                Result.failure(Exception("회원가입 실패: 서버에서 토큰을 받지 못했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
