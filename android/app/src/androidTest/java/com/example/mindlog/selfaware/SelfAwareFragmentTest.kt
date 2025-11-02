package com.example.mindlog.selfaware

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.features.selfaware.presentation.fragment.SelfAwareFragment
import com.example.mindlog.R
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltAndroidRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.example.mindlog.features.selfaware.domain.repository.SelfAwareRepository
import com.example.mindlog.utils.launchFragmentInHiltContainer
import dagger.hilt.android.testing.BindValue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SelfAwareFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Test
    fun showsQuestion_and_SubmitFlow_showsOverlay() {
        hiltRule.inject()

        launchFragmentInHiltContainer<SelfAwareFragment>(
            factory = { SelfAwareFragment() }
        )


        // 1) “오늘의 질문” 섹션 보임
        onView(withId(R.id.cardQuestion))
            .check(matches(isDisplayed()))
        onView(withId(R.id.groupQuestion))
            .check(matches(isDisplayed()))
        Thread.sleep(2000)
        // 2) FakeRepo가 즉시 질문을 제공 → 질문 TextView 표시
        onView(withId(R.id.tvQuestion))
            .check(matches(withText("오늘 하루 가장 의미 있었던 순간은?")))

        // 3) 답변 입력 → 버튼 활성화
        onView(withId(R.id.etAnswer))
            .perform(scrollTo(), replaceText("테스트 답변"), closeSoftKeyboard())
        onView(withId(R.id.btnSubmit))
            .check(matches(isEnabled()))
            .perform(scrollTo(), click())

        // 4) 제출 후 오버레이 노출
        onView(withId(R.id.completionOverlay))
            .check(matches(isDisplayed()))
        onView(withId(R.id.ivCheck))
            .check(matches(isDisplayed()))

        // 5) 가치 지도 카드/차트도 표시 (FakeRepo가 값 제공)
        onView(withId(R.id.cardValueMap))
            .check(matches(isDisplayed()))
        onView(withId(R.id.radar))
            .check(matches(isDisplayed()))

        // 6) 상위 가치 Chip 텍스트 확인
        onView(withId(R.id.chipValueFirst))
            .check(matches(withText("성장")))

        onView(withId(R.id.cardPersonalityInsight))
            .check(matches(isDisplayed()))
        onView(withId(R.id.tvPersonalityInsight))
            .check(matches(withText("호기심과 성취동기가 강해요.")))

        onView(withId(R.id.tvComment))
            .check(matches(withText("작은 성공을 빠르게 반복할 때 동기가 오래 유지됩니다.")))

        onView(withId(R.id.btnOpenHistory))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
    }
}