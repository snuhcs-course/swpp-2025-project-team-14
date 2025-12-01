package com.example.mindlog.features.auth.domain.usecase

import com.example.mindlog.features.auth.domain.repository.AuthRepository
import javax.inject.Inject

class RefreshTokenUseCase @Inject constructor(private val repo: AuthRepository) {
    suspend operator fun invoke() = repo.refresh()
}