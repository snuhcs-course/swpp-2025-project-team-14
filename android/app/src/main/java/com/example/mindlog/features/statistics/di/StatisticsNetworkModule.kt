package com.example.mindlog.features.statistics.di

import com.example.mindlog.features.statistics.data.api.StatisticsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class StatisticsNetworkModule {
    @Provides @Singleton
    fun provideStatisticsApi(retrofit: Retrofit): StatisticsApi =
        retrofit.create(StatisticsApi::class.java)
}

