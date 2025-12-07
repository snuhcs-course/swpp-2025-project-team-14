package com.example.mindlog.statistics

import android.view.View
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.UiController
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.statistics.di.StatisticsBindModule
import com.example.mindlog.features.statistics.presentation.StatisticsFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import com.github.mikephil.charting.charts.LineChart
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(StatisticsBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class StatisticsFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun initial_render_shows_neutral_reason_title_for_all_emotions() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        onView(withId(R.id.cardEmotionEvents)).perform(nestedScrollTo())
        Espresso.onIdle()

        // 초기 상태: 선택된 감정 없음(null) → "최근 그렇게 느꼈던 이유는?" 노출
        onView(withText("최근 그렇게 느꼈던 이유는?"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
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

    @Test
    fun word_cloud_is_rendered_when_keywords_exist() {
        // Given the fragment is launched with the test repository providing non-empty keywords
        launchFragmentInHiltContainer<StatisticsFragment>()

        onView(isRoot()).perform(swipeUp())
        // When: Scroll to the word cloud host view inside NestedScrollView
        onView(withId(R.id.wordCloudView)).perform(scrollTo())
        // Then: The word cloud container is visible
        onView(withId(R.id.wordCloudView)).check(matches(isDisplayed()))
    }

    @Test
    fun selecting_emotion_in_spinner_updates_title() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 감정 이벤트 카드까지 스크롤
        onView(withId(R.id.cardEmotionEvents)).perform(nestedScrollTo())
        Espresso.onIdle()
        // 초기 상태: 선택된 감정 없음(null) → "최근 그렇게 느꼈던 이유는?" 노출
        onView(withText("최근 그렇게 느꼈던 이유는?"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

        // 스피너에서 "슬픔" 선택 → 제목이 "최근 슬펐던 이유는?" 으로 변경되어야 함
        onView(withId(R.id.emotionSelectorContainer)).perform(nestedScrollTo())
        onView(withId(R.id.spinnerEmotions)).perform(click())
        onView(withText("슬픔"))
            .inRoot(isPlatformPopup())
            .perform(click())
        Espresso.onIdle()

        onView(withText("슬픔"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        Espresso.onIdle()

        onView(withId(R.id.cardEmotionEvents)).perform(nestedScrollTo())
        onView(withText("최근 슬펐던 이유는?"))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))

    }

    @Test
    fun period_preset_buttons_set_tvPeriodRange_with_correct_format() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // 주간 버튼 클릭 후 형식 검사: yyyy.MM.dd~yyyy.MM.dd
        onView(withId(R.id.btnWeekly)).perform(scrollTo(), click())
        onView(withId(R.id.tvPeriodRange)).check(matchesTextMatches("\\d{4}\\.\\d{2}\\.\\d{2} ~ \\d{4}\\.\\d{2}\\.\\d{2}"))

        // 월간 버튼 클릭 후에도 동일 형식 유지
        onView(withId(R.id.btnMonthly)).perform(scrollTo(), click())
        onView(withId(R.id.tvPeriodRange)).check(matchesTextMatches("\\d{4}\\.\\d{2}\\.\\d{2} ~ \\d{4}\\.\\d{2}\\.\\d{2}"))
    }

    @Test
    fun charts_have_minimal_data_after_render() {
        launchFragmentInHiltContainer<StatisticsFragment>()

        // PieChart 데이터 유효성 (data != null)
        onView(withId(R.id.chartEmotionRates)).check { view, _ ->
            val chart = view as com.github.mikephil.charting.charts.PieChart
            org.junit.Assert.assertNotNull(
                "PieChart data should not be null after render",
                chart.data
            )
        }

        // LineChart 데이터 유효성 (data != null)
        onView(withId(R.id.chartEmotionTrend)).check { view, _ ->
            val chart = view as LineChart
            org.junit.Assert.assertNotNull(
                "LineChart data should not be null after render",
                chart.data
            )
        }
    }

    // ---- helper ----
    private fun matchesTextMatches(regex: String) = ViewAssertion { view, _ ->
        val tv = view as TextView
        val ok = Regex(regex).matches(tv.text?.toString().orEmpty())
        org.junit.Assert.assertTrue("Text '${tv.text}' does not match pattern: $regex", ok)
    }

    private fun nestedScrollTo(): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> {
            // NestedScrollView 안에 있는 자식 뷰에서만 동작하도록 제한
            return isDescendantOfA(isAssignableFrom(NestedScrollView::class.java))
        }

        override fun getDescription(): String =
            "Scroll NestedScrollView to make the view fully visible"

        override fun perform(uiController: UiController, view: View) {
            var parent = view.parent
            while (parent is View && parent !is NestedScrollView) {
                parent = parent.parent
            }
            if (parent is NestedScrollView) {
                parent.smoothScrollTo(0, view.top)
                uiController.loopMainThreadUntilIdle()
            }
        }
    }
}

