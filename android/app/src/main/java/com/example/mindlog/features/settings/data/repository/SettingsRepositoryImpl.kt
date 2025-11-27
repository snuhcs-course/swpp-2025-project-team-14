package com.example.mindlog.features.settings.data.repository

import com.example.mindlog.core.model.UserInfo
import com.example.mindlog.features.settings.data.api.SettingsApi
import com.example.mindlog.features.settings.data.dto.PasswordUpdateRequest
import com.example.mindlog.features.settings.data.dto.UserUpdateRequest
import com.example.mindlog.features.settings.domain.repository.SettingsRepository
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val api: SettingsApi
) : SettingsRepository {

    override suspend fun getUserInfo(): UserInfo {
        val dto = api.getUserInfo()
        return UserInfo(
            id = dto.id,
            loginId = dto.loginId ?: "Unknown",
            username = dto.username ?: "사용자",
            gender = dto.gender,
            birthdate = dto.birthdate,
            appearance = dto.appearance
        )
    }

    override suspend fun updateUserInfo(
        username: String?,
        gender: String?,
        birthdate: String?,
        appearance: String?
    ) {
        val request = UserUpdateRequest(
            username = username,
            gender = gender,
            birthdate = birthdate,
            appearance = appearance
        )

        val response = api.updateUserInfo(request)
        if (!response.isSuccessful) {
            throw RuntimeException("내 정보 업데이트 실패: ${response.code()} ${response.message()}")
        }
    }

    override suspend fun updatePassword(currentPassword: String, newPassword: String) {
        val request = PasswordUpdateRequest(
            currentPassword = currentPassword,
            newPassword = newPassword
        )
        val response = api.updatePassword(request)

        if (!response.isSuccessful) {
            val errorMsg = response.errorBody()?.string() ?: response.message()
            throw RuntimeException(errorMsg)
        }
    }
}
