package com.example.mindlog.features.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AuthRepository {
    suspend fun login(loginId: String, password: String): Boolean
    suspend fun signup(loginId: String, password: String, username: String, gender: String, birthDate: LocalDate): Boolean
    suspend fun refresh(refresh: String): Boolean
    suspend fun verify(): Boolean
    suspend fun logout(): Boolean

    fun isLoggedInFlow(): Flow<Boolean>
}