package com.example.mindlog.features.settings.domain.usecase

import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class UpdatePasswordUseCase @Inject constructor(
    private val repository: SettingsRepository
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String) {
        repository.updatePassword(currentPassword, newPassword)
    }
}
