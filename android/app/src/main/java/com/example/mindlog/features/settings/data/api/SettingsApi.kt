package com.example.mindlog.features.settings.data.api

import com.example.mindlog.features.settings.data.dto.UserResponse
import com.example.mindlog.features.settings.data.dto.UserUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT

interface SettingsApi {
    // 내 정보 조회
    @GET("user/me")
    suspend fun getUserInfo(): UserResponse

    // 내 정보 업데이트 (성공 시 "Update Success" 문자열 반환)
    @PUT("user/me")
    suspend fun updateUserInfo(@Body request: UserUpdateRequest): Response<String>
}
