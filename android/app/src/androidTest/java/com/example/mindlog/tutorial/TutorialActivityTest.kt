package com.example.mindlog.tutorial


import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
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
        ActivityScenario.launch(TutorialActivity::class.java)
        // 처음 진입 시 버튼과 페이지 정보가 올바르게 표시되는지 확인
        onView(withId(R.id.btnNext))
            .check(matches(withText("다음")))

        onView(withId(R.id.tvPageInfo))
            .check(matches(withText(containsString("1/"))))
    }

    @Test
    fun clickNext_movesToNextPage() {
        ActivityScenario.launch(TutorialActivity::class.java)
        // 다음 버튼 클릭 시 페이지가 넘어가는지 (페이지 인덱스 증가) 확인
        onView(withId(R.id.btnNext)).perform(click())

        onView(withId(R.id.tvPageInfo))
            .check(matches(withText(containsString("2/"))))
    }

    @Test
    fun clickSkip_featureTutorial_setsTutorialCompleted_andFinishes() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = android.content.Intent(context, TutorialActivity::class.java).apply {
            // 메뉴에서 특정 기능 튜토리얼로 진입한 상황을 가정
            putExtra(TutorialActivity.EXTRA_FEATURE_LABEL, "일기 작성")
        }

        ActivityScenario.launch<TutorialActivity>(intent)

        val prefs = context.getSharedPreferences("tutorial_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        // 상단 "건너뛰기" 버튼 클릭
        onView(withId(R.id.btnSkip)).perform(click())
    }

    @Test
    fun finishTutorial_fromOnboarding_goToMenu_opensTutorialMenu() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = android.content.Intent(context, TutorialActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_GO_TO_MENU, true)
        }

        ActivityScenario.launch<TutorialActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.completeTutorialForTest()
            }
        }

        intended(hasComponent("com.example.mindlog.features.tutorial.TutorialMenuActivity"))
    }

    @Test
    fun featureTutorial_showsSelectedFeatureLabelInPageInfo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = android.content.Intent(context, TutorialActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_FEATURE_LABEL, "일기 작성")
        }

        ActivityScenario.launch<TutorialActivity>(intent)

        // 페이지 정보에 "일기 작성" 라벨이 포함되어 있는지 확인
        onView(withId(R.id.tvPageInfo))
            .check(matches(withText(containsString("일기 작성"))))
    }
}