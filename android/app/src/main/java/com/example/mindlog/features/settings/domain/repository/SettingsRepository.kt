package com.example.mindlog.features.settings.domain.repository

import com.example.mindlog.core.model.UserInfo

interface SettingsRepository {
    suspend fun getUserInfo(): UserInfo

    // 프로필 정보만 업데이트
    suspend fun updateUserInfo(
        username: String? = null,
        gender: String? = null,
        birthdate: String? = null,
        appearance: String? = null
    )
    suspend fun updatePassword(
        currentPassword: String,
        newPassword: String
    )
}