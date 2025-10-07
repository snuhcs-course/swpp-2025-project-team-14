package com.example.mindlog.features.auth.presentation.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.mindlog.databinding.ActivityLoginBinding
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.usecase.LoginUseCase
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel by viewModels<LoginViewModel> {
        val repository = AuthRepositoryImpl(applicationContext)
        val useCase = LoginUseCase(repository)
        LoginViewModelFactory(useCase)
    }

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
