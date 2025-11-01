package com.example.mindlog.selfaware

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasMinimumChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.mindlog.R
import com.example.mindlog.features.selfaware.presentation.fragment.SelfAwareFragment
import com.example.mindlog.features.selfaware.presentation.fragment.SelfAwareHistoryFragment
import com.example.mindlog.launchFragmentInHiltContainer
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SelfAwareHistoryFragmentUITest {

    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @Before fun setup() { hilt.inject() }

    @Test fun list_paginates_and_filters() {
        launchFragmentInHiltContainer<SelfAwareFragment>()

        // 첫 페이지
        onView(withId(R.id.recyclerAnswers))
            .check(matches(hasMinimumChildCount(1)))

        // 정렬 변경
        onView(withId(R.id.btnSortOldest)).perform(click())

        // 페이징: 바닥 스크롤
        onView(withId(R.id.recyclerAnswers))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(19))
            .perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(39))
    }

    @Test fun date_range_picker_opens() {
        launchFragmentInHiltContainer<SelfAwareHistoryFragment>()
        onView(withId(R.id.btnDateRange)).perform(click())
        onView(withText("기간 선택")).check(matches(isDisplayed()))
    }
}