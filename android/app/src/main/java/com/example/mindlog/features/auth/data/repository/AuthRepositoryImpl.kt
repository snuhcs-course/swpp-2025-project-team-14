package com.example.mindlog.features.auth.data.repository

import android.content.Context
import com.example.mindlog.features.auth.data.api.*
import com.example.mindlog.features.auth.domain.model.Token
import com.example.mindlog.features.auth.domain.repository.AuthRepository
import com.example.mindlog.features.auth.util.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class AuthRepositoryImpl(private val context: Context) : AuthRepository {

    private val tokenManager = TokenManager(context)
    private val api = RetrofitClient.getInstance(context)

    // ✅ 로그인
    override suspend fun login(loginId: String, password: String): Token? = withContext(Dispatchers.IO) {
        val response = api.login(LoginRequest(loginId = loginId, password = password)).execute()
        return@withContext if (response.isSuccessful) {
            response.body()?.data?.let {
                val token = Token(it.access, it.refresh)
                tokenManager.saveTokens(it.access, it.refresh)
                token
            }
        } else null
    }

    // ✅ 회원가입
    override suspend fun signup(loginId: String, password: String, username: String): Token? = withContext(Dispatchers.IO) {
        val response = api.signup(SignupRequest(loginId = loginId, password = password, username = username)).execute()
        return@withContext if (response.isSuccessful) {
            response.body()?.data?.let {
                val token = Token(it.access, it.refresh)
                tokenManager.saveTokens(it.access, it.refresh)
                token
            }
        } else null
    }

    // ✅ 토큰 갱신
    override suspend fun refreshToken(refresh: String): Token? = withContext(Dispatchers.IO) {
        val response = api.refreshToken(RefreshTokenRequest(refresh)).execute()
        return@withContext if (response.isSuccessful) {
            response.body()?.data?.let {
                val token = Token(it.access, it.refresh)
                tokenManager.saveTokens(it.access, it.refresh)
                token
            }
        } else null
    }

    // ✅ 토큰 유효성 검사
    override suspend fun verifyToken(): Boolean = withContext(Dispatchers.IO) {
        val token = tokenManager.getAccessToken() ?: return@withContext false
        try {
            val response = api.verifyToken("Bearer $token").execute()
            return@withContext response.isSuccessful
        } catch (e: HttpException) {
            return@withContext false
        } catch (e: Exception) {
            return@withContext false
        }
    }

    // ✅ 로그아웃
    override fun logout() {
        tokenManager.clearTokens()
    }
}
