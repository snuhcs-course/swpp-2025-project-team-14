package com.example.mindlog.features.auth.domain.repository

import com.example.mindlog.features.auth.domain.model.Token

interface AuthRepository {
    suspend fun login(loginId: String, password: String): Token?
    suspend fun signup(loginId: String, password: String, username: String): Token?
    suspend fun refreshToken(refresh: String): Token?

    suspend fun verifyToken(): Boolean

    fun logout()
}