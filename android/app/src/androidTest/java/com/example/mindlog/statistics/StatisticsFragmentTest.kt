package com.example.mindlog.statistics

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.features.statistics.domain.respository.StatisticsRepository
import com.example.mindlog.features.statistics.presentation.StatisticsFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.containsString
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun initial_render_shows_happy_events_then_chip_switches_to_sad() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 초기 선택: rates에서 HAPPY가 최대 → "최근 행복했던 이유는?" 노출
        onView(withText("최근 행복했던 이유는?"))
            .check(matches(isDisplayed()))

        // SAD 칩 클릭 → "최근 슬픔했던 이유는?"으로 변경
        onView(withId(R.id.chipSad)).perform(click())
        onView(withText("최근 슬픔했던 이유는?"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun clicking_weekly_and_monthly_updates_period_pill() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 주간 버튼 클릭 → 기간 pill에 "~" 포함 (yyyy.MM.dd~yyyy.MM.dd 형식)
        onView(withId(R.id.btnWeekly)).perform(click())
        onView(withId(R.id.tvPeriodRange))
            .check(matches(withText(containsString("~"))))

        // 월간 버튼 클릭 → 다시 갱신 확인
        onView(withId(R.id.btnMonthly)).perform(click())
        onView(withId(R.id.tvPeriodRange))
            .check(matches(withText(containsString("~"))))
    }

    @Test
    fun pie_and_line_charts_are_rendered() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 차트 뷰 자체가 존재하는지만 확인 (데이터는 페이크 레포가 공급)
        onView(withId(R.id.chartEmotionRates)).check(matches(isDisplayed()))
        onView(withId(R.id.chartEmotionTrend)).check(matches(isDisplayed()))
    }
}