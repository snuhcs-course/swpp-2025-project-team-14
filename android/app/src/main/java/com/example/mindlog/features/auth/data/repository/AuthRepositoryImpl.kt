package com.example.mindlog.features.auth.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.mindlog.features.auth.data.api.LoginRequest
import com.example.mindlog.features.auth.data.api.RetrofitClient
import com.example.mindlog.features.auth.data.api.SignupRequest
import com.example.mindlog.features.auth.data.api.TokenResponseEnvelope
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun login(id: String, password: String, callback: (Boolean, String?) -> Unit) {
        val request = LoginRequest(id, password)

        RetrofitClient.instance.login(request).enqueue(object : Callback<TokenResponseEnvelope> {
            override fun onResponse(
                call: Call<TokenResponseEnvelope>,
                response: Response<TokenResponseEnvelope>
            ) {
                if (response.isSuccessful) {
                    val tokenData = response.body()?.data
                    tokenData?.let {
                        saveTokens(it.access, it.refresh)
                        callback(true, null)
                    } ?: callback(false, "토큰이 없습니다")
                } else {
                    callback(false, "로그인 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<TokenResponseEnvelope>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    fun signup(id: String, password: String, name: String, callback: (Boolean, String?) -> Unit) {
        val request = SignupRequest(id, password, name)

        RetrofitClient.instance.signup(request).enqueue(object : Callback<TokenResponseEnvelope> {
            override fun onResponse(
                call: Call<TokenResponseEnvelope>,
                response: Response<TokenResponseEnvelope>
            ) {
                if (response.isSuccessful) {
                    val tokenData = response.body()?.data
                    tokenData?.let {
                        saveTokens(it.access, it.refresh)
                        callback(true, null)
                    } ?: callback(false, "토큰이 없습니다")
                } else {
                    callback(false, "회원가입 실패: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<TokenResponseEnvelope>, t: Throwable) {
                callback(false, t.message)
            }
        })
    }

    private fun saveTokens(access: String, refresh: String) {
        prefs.edit().apply {
            putString("access_token", access)
            putString("refresh_token", refresh)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)
    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun logout() {
        prefs.edit { clear() }
    }
}