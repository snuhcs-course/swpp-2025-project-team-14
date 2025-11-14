package com.example.mindlog.auth

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.auth.di.AuthBindModule
import com.example.mindlog.features.auth.presentation.main.MainActivity
import com.example.mindlog.features.auth.presentation.signup.SignupActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject


@HiltAndroidTest
@UninstallModules(AuthBindModule::class, DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class SignupActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var testAuthRepository: TestAuthRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun signup_success_navigates_to_main_activity() {
        // given
        testAuthRepository.shouldSignupSucceed = true

        Intents.init()
        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        onView(withId(R.id.etUsername))
            .perform(typeText("test_username"), closeSoftKeyboard())

        onView(withId(R.id.etLoginId))
            .perform(typeText("test_id"), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.etConfirmPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.rbMale)).perform(click())

        onView(withId(R.id.actBirthYear))
            .perform(replaceText("2000"), closeSoftKeyboard())
        onView(withId(R.id.actBirthMonth))
            .perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.actBirthDay))
            .perform(replaceText("1"), closeSoftKeyboard())

        onView(withId(R.id.btnSignup)).perform(click())

        // then: MainActivity로 이동하는 인텐트가 발생했는지 확인
        Intents.intended(hasComponent(MainActivity::class.java.name))

        scenario.close()
        Intents.release()
    }

    @Test
    fun signup_fail_stays_on_signup_screen() {
        // given
        testAuthRepository.shouldSignupSucceed = false

        val scenario = ActivityScenario.launch(SignupActivity::class.java)

        onView(withId(R.id.etUsername))
            .perform(typeText("test_username"), closeSoftKeyboard())

        onView(withId(R.id.etLoginId))
            .perform(typeText("test_id"), closeSoftKeyboard())

        onView(withId(R.id.etPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.etConfirmPassword))
            .perform(typeText("password123"), closeSoftKeyboard())

        onView(withId(R.id.rbMale)).perform(click())

        onView(withId(R.id.actBirthYear))
            .perform(replaceText("2000"), closeSoftKeyboard())
        onView(withId(R.id.actBirthMonth))
            .perform(replaceText("1"), closeSoftKeyboard())
        onView(withId(R.id.actBirthDay))
            .perform(replaceText("1"), closeSoftKeyboard())

        onView(withId(R.id.btnSignup)).perform(click())

        // then: 여전히 회원가입 화면에 머물러 있는지 (ID 입력 필드가 보이는지) 확인
        onView(withId(R.id.etLoginId)).check(matches(isDisplayed()))

        scenario.close()
    }
}