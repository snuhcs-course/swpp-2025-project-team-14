package com.example.mindlog.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("/signup")
    fun signup(@Body request: SignupRequest): Call<SignupResponse>
}

// 요청/응답 데이터 클래스
data class LoginRequest(val id: String, val password: String)
data class LoginResponse(val success: Boolean, val message: String)

data class SignupRequest(val id: String, val password: String, val username: String)
data class SignupResponse(val success: Boolean, val message: String)