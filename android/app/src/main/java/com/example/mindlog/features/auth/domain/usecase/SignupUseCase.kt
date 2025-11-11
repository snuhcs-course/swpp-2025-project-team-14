package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import java.time.LocalDate
import javax.inject.Inject

class SignupUseCase @Inject constructor(
    private val repository: AuthRepository
) {

    suspend operator fun invoke(loginId: String, password: String, username: String, gender: String, birthDate: LocalDate): Result<Boolean> {
        return try {
            val isSuccess = repository.signup(loginId, password, username, gender, birthDate)
            if (isSuccess) {
                Result.success(isSuccess)
            } else {
                Result.failure(Exception("회원가입 실패: 서버에서 토큰을 받지 못했습니다."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
