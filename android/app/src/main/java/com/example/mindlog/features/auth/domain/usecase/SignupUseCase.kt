package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import java.time.LocalDate
import javax.inject.Inject

class SignupUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke(loginId: String, password: String, username: String, gender: String, birthDate: LocalDate) = repo.signup(loginId, password, username, gender, birthDate)
}