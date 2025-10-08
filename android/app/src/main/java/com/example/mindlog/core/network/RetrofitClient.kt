package com.example.mindlog.core.network

import com.example.mindlog.BuildConfig
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    /**
     * Create a shared OkHttpClient.
     */
    fun createClient(
        authInterceptor: Interceptor? = null,
        tokenAuthenticator: Authenticator? = null,
        configure: (OkHttpClient.Builder.() -> Unit)? = null
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
        if (authInterceptor != null) builder.addInterceptor(authInterceptor)
        if (tokenAuthenticator != null) builder.authenticator(tokenAuthenticator)
        configure?.invoke(builder)
        return builder.build()
    }

    /**
     * Create a Retrofit instance using the shared OkHttpClient.
     * Default baseUrl comes from BuildConfig
     */
    fun createRetrofit(
        client: OkHttpClient,
        baseUrl: String = BuildConfig.API_BASE_URL
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}