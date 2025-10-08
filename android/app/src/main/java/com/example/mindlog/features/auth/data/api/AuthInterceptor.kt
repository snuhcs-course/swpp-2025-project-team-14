package com.example.mindlog.features.auth.data.api

import okhttp3.Interceptor
import okhttp3.Response
import com.example.mindlog.features.auth.util.TokenManager


class AuthInterceptor constructor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        // Not post token if there is No-Auth marker
        if (request.header("No-Auth") == "true") {
            return chain.proceed(request.newBuilder().removeHeader("No-Auth").build())
        }

        val access = tokenManager.getAccessToken()
        val authorized_request = if (!access.isNullOrEmpty()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $access")
                .build()
        } else {
            request
        }

        return chain.proceed(authorized_request)
    }
}