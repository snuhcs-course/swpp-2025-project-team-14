package com.example.mindlog.features.auth.domain.repository

import com.example.mindlog.features.auth.domain.model.Token

interface AuthRepository {
    suspend fun login(id: String, password: String): Token?
    suspend fun signup(id: String, password: String, name: String): Token?
    fun logout()
}