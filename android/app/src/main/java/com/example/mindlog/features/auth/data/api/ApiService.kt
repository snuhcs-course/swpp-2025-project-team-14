package com.example.mindlog.features.auth.data.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    // 회원가입
    @POST("auth/signup")
    fun signup(@Body request: SignupRequest): Call<TokenResponseEnvelope>

    // 로그인
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<TokenResponseEnvelope>

    // AccessToken 갱신 (refreshToken 이용)
    @POST("auth/refresh")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<TokenResponseEnvelope>

    // AccessToken 유효성 검사
    @POST("auth/verify")
    fun verifyToken(@Header("Authorization") bearerToken: String): Call<Void>
}
