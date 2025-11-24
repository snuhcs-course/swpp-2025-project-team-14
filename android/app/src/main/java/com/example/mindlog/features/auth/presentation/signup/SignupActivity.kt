package com.example.mindlog.features.auth.presentation.signup

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.core.common.SystemUiHelper
import com.example.mindlog.databinding.ActivitySignupBinding
import com.example.mindlog.features.auth.presentation.login.LoginActivity
import com.example.mindlog.features.auth.presentation.main.MainActivity
import com.google.android.material.textfield.TextInputLayout
import java.time.YearMonth

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemUiHelper.hideSystemUI(this)

        // 힌트 조작 리스너 제거 (XML 힌트만 사용)
        // (기존 setOnFocusChangeListener 4개 전부 삭제)

        // 생년월일 드롭다운 어댑터 연결
        setupBirthDropdowns()

        // 확인 비밀번호 endIcon 동적 전환
        setupConfirmPasswordEndIcon()

        // 회원가입 버튼
        binding.btnSignup.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val id = binding.etLoginId.text.toString().trim()
            val pw = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()

            val gender = when (binding.rgGender.checkedRadioButtonId) {
                com.example.mindlog.R.id.rbMale -> "Male"
                com.example.mindlog.R.id.rbFemale -> "Female"
                else -> null
            }
            val birthY = binding.actBirthYear.text.toString().toIntOrNull()
            val birthM = binding.actBirthMonth.text.toString().toIntOrNull()
            val birthD = binding.actBirthDay.text.toString().toIntOrNull()

            if (username.isBlank() || id.isBlank() || pw.isBlank()) {
                Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pw != confirm) {
                Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (gender == null) {
                Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (birthY == null || birthM == null || birthD == null) {
                Toast.makeText(this, "생년월일을 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // (선택) 존재하지 않는 날짜 방지 체크
            if (!isValidDate(birthY, birthM, birthD)) {
                Toast.makeText(this, "유효하지 않은 생년월일입니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val birthDate = java.time.LocalDate.of(birthY, birthM, birthD)
            viewModel.signup(id, pw, username, gender, birthDate)
        }

        // 로그인 화면 이동
        binding.tvGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // ViewModel 관찰
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

    private fun setupBirthDropdowns() {
        val currentYear = java.time.LocalDate.now().year
        val years = (1900..currentYear).toList().reversed().map { it.toString() }
        val months = (1..12).map { it.toString() }
        val days31 = (1..31).map { it.toString() }

        binding.actBirthYear.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, years))
        binding.actBirthMonth.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, months))
        binding.actBirthDay.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, days31))

        fun daysInMonth(y: Int, m: Int) = YearMonth.of(y, m).lengthOfMonth()

        fun updateDays() {
            val y = binding.actBirthYear.text.toString().toIntOrNull() ?: return
            val m = binding.actBirthMonth.text.toString().toIntOrNull() ?: return
            val maxD = daysInMonth(y, m)
            val dayList = (1..maxD).map { it.toString() }
            binding.actBirthDay.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, dayList))
            val cur = binding.actBirthDay.text.toString().toIntOrNull()
            if (cur == null || cur > maxD) binding.actBirthDay.setText("")
        }

        binding.actBirthYear.setOnItemClickListener { _, _, _, _ -> updateDays() }
        binding.actBirthMonth.setOnItemClickListener { _, _, _, _ -> updateDays() }
    }

    private fun setupConfirmPasswordEndIcon() {
        val tilConfirm = binding.tilConfirmPassword
        val etConfirm = binding.etConfirmPassword

        etConfirm.setOnFocusChangeListener { _, hasFocus ->
            tilConfirm.endIconMode = if (hasFocus)
                TextInputLayout.END_ICON_PASSWORD_TOGGLE
            else
                TextInputLayout.END_ICON_CLEAR_TEXT
        }
    }

    private fun isValidDate(y: Int, m: Int, d: Int): Boolean {
        return try {
            YearMonth.of(y, m).atDay(d)  // 예외 던지면 invalid
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) SystemUiHelper.hideSystemUI(this)
    }
}
