package com.example.mindlog.features.journal.di

import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.repository.JournalRepositoryImpl
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object JournalModule {

    /**
     * JournalRepository 인터페이스에 대한 구현체를 제공합니다.
     *
     * Hilt에게 JournalRepository 타입이 요청되면,
     * JournalRepositoryImpl 인스턴스를 생성하여 주입하도록 알려줍니다.
     */
    @Provides
    @Singleton
    fun provideJournalRepository(journalApi: JournalApi): JournalRepository {
        return JournalRepositoryImpl(journalApi)
    }

    /**
     * Retrofit을 사용하여 JournalApi 인터페이스의 구현체를 생성합니다.
     *
     * Hilt에게 JournalApi 타입이 요청되면,
     * 주입받은 Retrofit 객체를 사용하여 JournalApi의 인스턴스를 생성하도록 알려줍니다.
     * (NetworkModule에서 Retrofit 객체를 주입받아 사용합니다)
     */
    @Provides
    @Singleton
    fun provideJournalApi(retrofit: Retrofit): JournalApi {
        return retrofit.create(JournalApi::class.java)
    }
}

