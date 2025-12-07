package com.example.mindlog.features.statistics.di

import com.example.mindlog.features.statistics.data.repository.StatisticsRepositoryImpl
import com.example.mindlog.features.statistics.domain.repository.StatisticsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StatisticsBindModule {
    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(impl: StatisticsRepositoryImpl): StatisticsRepository
}