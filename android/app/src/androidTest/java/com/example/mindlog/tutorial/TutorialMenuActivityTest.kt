package com.example.mindlog.tutorial

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.TutorialActivity
import com.example.mindlog.features.tutorial.TutorialMenuActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class TutorialMenuActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
        Intents.init()
    }

    @After
    fun teardown() {
        Intents.release()
    }

    @Test
    fun menu_showsList_andClickFirstItem_opensTutorialActivityWithFeatureExtra() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, TutorialMenuActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, false)
        }

        ActivityScenario.launch<TutorialMenuActivity>(intent)

        // 메뉴 리스트가 보이는지 기본 체크
        onView(withId(R.id.rvTutorialMenu)).check(matches(isDisplayed()))

        // 첫 번째 아이템 클릭 → TutorialActivity 열리고, feature extra 가 "일기 작성" 인지 확인
        onView(withId(R.id.rvTutorialMenu))
            .perform(
                RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(
                    0,
                    click()
                )
            )

        intended(
            hasComponent(TutorialActivity::class.java.name)
        )
        intended(
            hasExtra(TutorialActivity.EXTRA_FEATURE_LABEL, "일기 작성")
        )
    }

    @Test
    fun clickFinish_fromOnboarding_opensHomeActivity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, TutorialMenuActivity::class.java).apply {
            // 온보딩에서 진입하는 경우: returnToSettings = false
            putExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, false)
        }

        ActivityScenario.launch<TutorialMenuActivity>(intent)

        onView(withId(R.id.btnFinishTutorialMenu)).perform(click())
        // 첫 번째 클릭: 엔딩(캐릭터) 화면으로 전환
        onView(withId(R.id.finishContainer)).check(matches(isDisplayed()))
        // 두 번째 클릭: 실제로 HomeActivity 로 이동
        onView(withId(R.id.btnFinishTutorialMenu)).perform(click())
        intended(hasComponent(HomeActivity::class.java.name))
    }

    @Test
    fun clickFinish_fromSettings_finishesWithoutOpeningHome() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, TutorialMenuActivity::class.java).apply {
            // 설정에서 진입하는 경우: returnToSettings = true
            putExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, true)
        }

        val scenario = ActivityScenario.launch<TutorialMenuActivity>(intent)

        onView(withId(R.id.btnFinishTutorialMenu)).perform(click())
    }
}