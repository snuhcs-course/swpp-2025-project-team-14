package com.example.mindlog.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.R
import com.example.mindlog.api.RetrofitClient
import com.example.mindlog.api.SignupRequest
import com.example.mindlog.api.SignupResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etID = findViewById<EditText>(R.id.etSignupID)
        val etPassword = findViewById<EditText>(R.id.etSignupPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)

        btnSignup.setOnClickListener {
            val id = etID.text.toString()
            val password = etPassword.text.toString()
            val confirmpassword = etConfirmPassword.text.toString()

            if (id.isNotEmpty() && password.isNotEmpty() && confirmpassword.isNotEmpty()) {
                val request = SignupRequest(id, password)

                RetrofitClient.instance.signup(request)
                    .enqueue(object : Callback<SignupResponse> {
                        override fun onResponse(call: Call<SignupResponse>, response: Response<SignupResponse>) {
                            if (response.isSuccessful && response.body()?.success == true) {
                                Toast.makeText(this@SignupActivity, "회원가입 성공", Toast.LENGTH_SHORT).show()
                                finish() // 회원가입 후 로그인 화면으로 복귀
                            } else {
                                Toast.makeText(this@SignupActivity, "회원가입 실패", Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                            Toast.makeText(this@SignupActivity, "서버 연결 실패: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            } else {
                Toast.makeText(this, "모든 항목을 입력해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}