package com.example.mindlog.features.auth.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/signup")
    fun signup(@Body request: SignupRequest): Call<TokenResponseEnvelope>

    @POST("/login")
    fun login(@Body request: LoginRequest): Call<TokenResponseEnvelope>

    @POST("/refresh")
    fun refresh(@Body request: RefreshTokenRequest): Call<TokenResponseEnvelope>
}
