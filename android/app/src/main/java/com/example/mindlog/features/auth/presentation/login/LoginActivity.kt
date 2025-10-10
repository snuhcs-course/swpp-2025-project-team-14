package com.example.mindlog.features.auth.presentation.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.mindlog.core.network.NetworkModule
import com.example.mindlog.databinding.ActivityLoginBinding
import com.example.mindlog.features.auth.data.api.AuthApi
import com.example.mindlog.features.auth.data.api.RefreshApi
import com.example.mindlog.features.auth.data.network.AuthInterceptor
import com.example.mindlog.features.auth.data.network.TokenAuthenticator
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.util.TokenManager
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val id = binding.etLoginId.text.toString()
            val password = binding.etPassword.text.toString()

            if (id.isBlank() || password.isBlank()) {
                Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(id, password)
        }

        binding.etLoginId.setOnFocusChangeListener { _, hasFocus ->
            binding.etLoginId.hint = if (hasFocus) "" else "아이디를 입력하세요"
        }
        binding.etPassword.setOnFocusChangeListener { _, hasFocus ->
            binding.etPassword.hint = if (hasFocus) "" else "비밀번호를 입력하세요"
        }

        binding.tvGoSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        viewModel.loginResult.observe(this, Observer { success ->
            if (success) {
                Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                Toast.makeText(this, "로그인 실패. 아이디와 비밀번호를 확인하세요.", Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.errorMessage.observe(this, Observer { message ->
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        })
    }
}
