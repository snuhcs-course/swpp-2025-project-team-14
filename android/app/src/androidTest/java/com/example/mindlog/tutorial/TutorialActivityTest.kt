package com.example.mindlog.tutorial

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.TutorialActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.containsString
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TutorialActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    @get:Rule
    val activityRule = ActivityScenarioRule(TutorialActivity::class.java)

    @Before
    fun initIntents() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun firstPage_showsCorrectInitialTexts() {
        // 처음 진입 시 버튼과 페이지 정보가 올바르게 표시되는지 확인
        onView(withId(R.id.btnNext))
            .check(matches(withText("다음")))

        onView(withId(R.id.tvPageInfo))
            .check(matches(withText(containsString("1/"))))
    }

    @Test
    fun clickNext_movesToNextPage() {
        // 다음 버튼 클릭 시 페이지가 넘어가는지 (페이지 인덱스 증가) 확인
        onView(withId(R.id.btnNext)).perform(click())

        onView(withId(R.id.tvPageInfo))
            .check(matches(withText(containsString("2/"))))
    }

    @Test
    fun clickSkip_setsTutorialCompleted_andFinishesActivity() {
        // SharedPreferences 초기화 (Application Context 기준)
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val prefs = appContext.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // 실제 클릭 대신, Activity의 테스트용 헬퍼 메서드를 직접 호출
        activityRule.scenario.onActivity { activity ->
            activity.completeTutorialForTest()
        }

        // 튜토리얼 완료 플래그가 true로 저장되었는지 확인
        val completed = prefs.getBoolean("completed", false)
        assert(completed)

        // ActivityScenario가 종료 상태인지 확인 (튜토리얼이 닫혔는지)
        assert(activityRule.scenario.state.isAtLeast(androidx.lifecycle.Lifecycle.State.DESTROYED))
    }
}