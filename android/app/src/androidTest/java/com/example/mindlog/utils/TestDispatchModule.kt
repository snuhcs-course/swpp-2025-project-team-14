package com.example.mindlog.utils

import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.core.dispatcher.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DispatcherModule::class]
)
object TestDispatcherModule {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Provides @Singleton
    fun provideDispatcherProvider(): DispatcherProvider {
        // UI 테스트에서 가상 시간 제어가 필요 없다면 이렇게 간단히
        return TestDispatcherProvider(StandardTestDispatcher())
    }
}