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
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}