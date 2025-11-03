package com.example.mindlog.selfaware

import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.data.api.SelfAwareApi
import com.example.mindlog.features.selfaware.data.mapper.SelfAwareMapper
import com.example.mindlog.features.selfaware.data.repository.SelfAwareRepositoryImpl
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import javax.inject.Inject

@HiltAndroidTest
class SelfAwareRepositoryIntegrationTest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)

    @Inject lateinit var server: MockWebServer
    @Inject lateinit var api: SelfAwareApi
    @Inject lateinit var mapper: SelfAwareMapper
    @Inject lateinit var dispatcher: DispatcherProvider
    private lateinit var repo: SelfAwareRepository

    @Before fun setup() {
        hilt.inject()
        server.dispatcher = SelfAwareDispatcher(this::class.java)
        repo = SelfAwareRepositoryImpl(api, mapper, dispatcher)
    }

    @Test fun getToday_calls_correct_path_and_parses() = runTest {
        val res = repo.getTodayQA(LocalDate.parse("2025-10-21"))
        assertTrue(res is com.example.mindlog.core.common.Result.Success)

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertTrue(req.path!!.contains("/selfaware/daily"))
        assertTrue(req.path!!.contains("date=2025-10-21"))
    }

    @After fun tearDown() { server.shutdown() }
}