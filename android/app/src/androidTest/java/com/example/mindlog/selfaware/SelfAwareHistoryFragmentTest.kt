package com.example.mindlog.selfaware

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.selfaware.di.SelfAwareBindModule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(SelfAwareBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class SelfAwareHistoryFragmentTest {
}