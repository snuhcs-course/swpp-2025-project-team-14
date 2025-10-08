package com.example.mindlog.core.network

import android.content.Context
import com.example.mindlog.features.auth.data.api.ApiService
import com.example.mindlog.features.auth.util.TokenManager
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "http://ec2-15-164-239-56.ap-northeast-2.compute.amazonaws.com:3000/"

    // Retrofit 인스턴스 생성
    fun getInstance(context: Context): ApiService {
        val tokenManager = TokenManager(context)
        val authInterceptor = AuthInterceptor(tokenManager)
        val authenticator = TokenAuthenticator(tokenManager)
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(authenticator)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val accessToken = tokenManager.getAccessToken()
        val requestBuilder = chain.request().newBuilder()

        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}

class TokenAuthenticator(private val tokenManager: TokenManager) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val refreshToken = tokenManager.getRefreshToken() ?: return null

        // refreshToken 요청
        val api = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val newTokenResponse = api.refreshToken(RefreshTokenRequest(refreshToken)).execute()

        if (newTokenResponse.isSuccessful) {
            val newTokens = newTokenResponse.body()
            val newAccess = newTokens?.data?.access ?: return null
            val newRefresh = newTokens?.data?.refresh ?: refreshToken

            tokenManager.saveTokens(newAccess, newRefresh)

            return response.request()
                .newBuilder()
                .header("Authorization", "Bearer $newAccess")
                .build()
        }

        return null
    }
}