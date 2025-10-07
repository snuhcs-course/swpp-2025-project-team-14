package com.example.mindlog.features.auth.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.databinding.ActivitySignupBinding
import com.example.mindlog.features.auth.data.repository.AuthRepositoryImpl
import com.example.mindlog.features.auth.domain.usecase.SignupUseCase
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel by viewModels<SignupViewModel> {
        val repository = AuthRepositoryImpl(applicationContext)
        val useCase = SignupUseCase(repository)
        SignupViewModelFactory(useCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 🔹 회원가입 버튼 클릭
        binding.btnSignup.setOnClickListener {
            val username = binding.etSignupUsername.text.toString()
            val id = binding.etSignupId.text.toString()
            val pw = binding.etSignupPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            if (id.isBlank() || pw.isBlank() || username.isBlank()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pw != confirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.signup(id, pw, username)
        }

        // 🔹 로그인 화면 이동
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // 🔹 회원가입 결과 관찰
        viewModel.signupResult.observe(this) { success ->
            if (success) {
                Toast.makeText(this, "회원가입 성공! 자동 로그인 중...", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finishAffinity()
            } else {
                Toast.makeText(this, "회원가입 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { msg ->
            msg?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }
    }
}
