package com.example.mindlog.features.auth.presentation.main

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mindlog.core.network.RetrofitClient
import com.example.mindlog.databinding.ActivityMainBinding
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.api.AuthInterceptor
import com.example.mindlog.features.auth.data.api.TokenAuthenticator
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
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

        Log.d("MainActivity", "AuthRepository initialized. Checking for auto-login")

        tokenManager = TokenManager(this)
        // 1) bare client/retrofit for refresh
        val bareClient = RetrofitClient.createClient()
        val bareRetrofit = RetrofitClient.createRetrofit(bareClient)
        val refreshApi = bareRetrofit.create(RefreshApi::class.java)

        // 2) main client/retrofit with auth pipeline
        val authInterceptor = AuthInterceptor(tokenManager)
        val tokenAuthenticator = TokenAuthenticator(tokenManager, refreshApi)
        val okHttp = RetrofitClient.createClient(
            authInterceptor = authInterceptor,
            tokenAuthenticator = tokenAuthenticator
        )
        val retrofit = RetrofitClient.createRetrofit(okHttp)
        val authApi = retrofit.create(AuthApi::class.java)

        // 3) repository (manual DI)
        authRepository = AuthRepositoryImpl(
            authApi = authApi,
            refreshApi = refreshApi,
            tokenManager = tokenManager
        )

        lifecycleScope.launch {
            checkAutoLogin()
        }
    }

    private suspend fun checkAutoLogin() {
        val access = tokenManager.getAccessToken()
        val refresh = tokenManager.getRefreshToken()

        when {
            access.isNullOrEmpty() || refresh.isNullOrEmpty() -> {
                // 토큰 없음 → 로그인 화면으로
                goToLogin()
            }
            tokenManager.isAccessTokenExpired() -> {
                // 토큰 만료 → refresh 시도
                refresh.let { token ->
                    if (authRepository.refresh(token)) {
                        goToJournal()
                    } else {
                        tokenManager.clearTokens()
                        goToLogin()
                    }
                }
            }
            else -> {
                if (authRepository.verify()) {
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
        startActivity(Intent(this, SignupActivity::class.java))
        finish()
    }
}
