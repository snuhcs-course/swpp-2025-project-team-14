package com.example.mindlog.analysis

import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.mindlog.R
import com.example.mindlog.features.analysis.presentation.AnalysisFragment
import com.example.mindlog.utils.launchFragmentInHiltContainer
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.UninstallModules
import com.example.mindlog.features.analysis.di.AnalysisBindModule
import com.example.mindlog.core.dispatcher.DispatcherModule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@UninstallModules(AnalysisBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class AnalysisFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun loadsData_and_showsAllCards() {
        launchFragmentInHiltContainer<AnalysisFragment>()

        // 1) User Type 카드 보임
        onView(withId(R.id.cardUserType))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvUserTypeName))
            .perform(scrollTo())
            .check(matches(withText("탐험가형")))

        onView(withId(R.id.tvUserTypeDescription))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // 캐릭터 이미지 표시됨
        onView(withId(R.id.ivUserTypeCharacter))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // 2) Five Factor 카드
        onView(withId(R.id.cardComprehensive))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvConscientiousness))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvOpenness))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        // 3) Personalized Advice 카드
        onView(withId(R.id.cardAdvice))
            .perform(scrollTo())
            .check(matches(isDisplayed()))

        onView(withId(R.id.tvAdviceType))
            .perform(scrollTo())
            .check(matches(withText("오늘의 탐험 가이드")))
    }

    @Test
    fun navigation_check_from_analysis() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val navController = TestNavHostController(context)

        launchFragmentInHiltContainer<AnalysisFragment> {
            navController.setGraph(R.navigation.nav_graph_home)
            navController.setCurrentDestination(R.id.analysisFragment)
            Navigation.setViewNavController(requireView(), navController)
        }

        // 만약 클릭 가능한 요소가 있다면 클릭 — 여기서는 타입 카드 전체를 클릭한다고 가정
        onView(withId(R.id.cardUserType))
            .check(matches(isDisplayed()))

        // 현재 Fragment가 제대로 세팅되어 있는지만 검사
        assertThat(navController.currentDestination?.id)
            .isEqualTo(R.id.analysisFragment)
    }
}