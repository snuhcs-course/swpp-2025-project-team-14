package com.example.mindlog.features.auth.presentation.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.R
import com.example.mindlog.databinding.ActivityMainBinding
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.util.TokenManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var tokenManager: TokenManager
    private lateinit var authRepository: AuthRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tokenManager = TokenManager(this)
        authRepository = AuthRepositoryImpl(this)

        lifecycleScope.launch {
            checkAutoLogin()
        }
    }

    private suspend fun checkAutoLogin() {
        val accessToken = tokenManager.getAccessToken()
        val refreshToken = tokenManager.getRefreshToken()

        when {
            accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty() -> {
                // 토큰 없음 → 로그인 화면으로
                goToLogin()
            }
            tokenManager.isAccessTokenExpired() -> {
                // 토큰 만료 → refresh 시도
                val newToken = authRepository.refreshToken(refreshToken!!)
                if (newToken != null) {
                    // refresh 성공 → 저장 후 Journal 화면으로
                    tokenManager.saveTokens(newToken.accessToken, newToken.refreshToken)
                    goToJournal()
                } else {
                    // refresh 실패 → 로그인 필요
                    tokenManager.clearTokens()
                    goToLogin()
                }
            }
            else -> {
                val isValid = authRepository.verifyToken()
                if (isValid) {
                    goToJournal()
                } else {
                    tokenManager.clearTokens()
                    goToLogin()
                }
            }
        }
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun goToJournal() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
