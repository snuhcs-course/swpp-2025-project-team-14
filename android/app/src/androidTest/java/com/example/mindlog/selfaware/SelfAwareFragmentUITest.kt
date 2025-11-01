package com.example.mindlog.selfaware

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.example.mindlog.R
import com.example.mindlog.features.selfaware.presentation.fragment.SelfAwareFragment
import com.example.mindlog.launchFragmentInHiltContainer
import com.example.mindlog.utils.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class SelfAwareFragmentUITest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val mainRule = MainDispatcherRule()
    @Inject lateinit var server: MockWebServer

    @Before fun setup() { hilt.inject(); server.dispatcher = SelfAwareDispatcher(this::class.java) }

    @Test fun load_question_and_submit() {
        launchFragmentInHiltContainer<SelfAwareFragment>()

        onView(withId(R.id.tvQuestion)).check(matches(isDisplayed()))
        onView(withId(R.id.etAnswer)).perform(typeText("테스트 답변"))
        onView(withId(R.id.btnSubmit)).perform(click())
        onView(withId(R.id.btnSubmit)).check(matches(not(isEnabled())))
    }
}