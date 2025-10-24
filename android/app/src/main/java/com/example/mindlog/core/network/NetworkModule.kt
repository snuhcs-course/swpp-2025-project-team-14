package com.example.mindlog.core.network

import com.example.mindlog.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // Create a shared OkHttpClient
    @Provides @Singleton
    fun createClient(
        interceptor: Interceptor,
        authenticator: Authenticator
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .authenticator(authenticator)
        .build()

    /**
     * Create a Retrofit instance using the shared OkHttpClient.
     * Default baseUrl comes from BuildConfig
     */
    @Provides @Singleton
    fun createRetrofit(
        client: OkHttpClient,
    ): Retrofit {
        // --- 이 부분이 핵심입니다 ---
        // 1. 로컬 PC에서 실행 중인 Docker 서버의 주소.
        //    (포트: 3000, 기본 경로: /api/v1/)
        val localApiUrl = "http://10.0.2.2:3000/api/v1/"

        // 2. EC2 주소 대신, 테스트를 위해 임시로 로컬 주소를 사용하도록 변경.
        return Retrofit.Builder()
            .baseUrl(localApiUrl) // BuildConfig.API_BASE_URL 대신 localApiUrl 사용
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}