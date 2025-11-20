package com.example.mindlog.journal

import android.app.Activity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalWriteActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()
    }

    @Test
    fun writeJournal_success_finishes_activity_with_result_ok() {
        // 1. Activity 실행 (결과를 받기 위해 launchActivityForResult 사용)
        // 주의: 이 메서드는 Deprecated 되었으나, scenario.result를 확실하게 받기 위해 사용합니다.
        @Suppress("DEPRECATION")
        val scenario = ActivityScenario.launchActivityForResult(JournalWriteActivity::class.java)

        // 2. 감정 선택 화면 (EmotionSelectFragment)
        // '다음' 버튼 클릭
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 3. 내용 작성 화면 (ContentWriteFragment)
        onView(withId(R.id.et_title)).perform(replaceText("Activity 테스트 제목"))
        onView(withId(R.id.et_content)).perform(replaceText("Activity 테스트 내용"))

        // 저장 버튼 클릭 (클릭 즉시 finish() 로직이 수행됨)
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 4. 검증
        // Activity가 정상적으로 종료되었고 RESULT_OK를 반환했는지를 확인합니다.
        val result = scenario.result
        assert(result.resultCode == Activity.RESULT_OK)

        scenario.close()
    }

    @Test
    fun writeJournal_empty_title_shows_error_toast_and_stays() {
        // 실패 케이스는 결과 확인이 필요 없으므로 일반 launch 사용
        val scenario = ActivityScenario.launch(JournalWriteActivity::class.java)

        // 감정 선택 후 이동
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 제목 비우고 내용만 입력
        onView(withId(R.id.et_title)).perform(replaceText(""))
        onView(withId(R.id.et_content)).perform(replaceText("내용만 있음"))

        // 저장 시도
        onView(withId(R.id.btn_next_or_save)).perform(click())

        // 4. 검증
        // 여전히 입력창이 화면에 표시되어야 함
        onView(withId(R.id.et_content)).check(matches(isDisplayed()))

        // Activity 상태가 DESTROYED가 아니어야 함 (여전히 실행 중이어야 함)
        assertTrue(scenario.state != Lifecycle.State.DESTROYED)

        scenario.close()
    }
}
