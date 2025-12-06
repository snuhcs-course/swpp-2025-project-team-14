package com.example.mindlog.journal

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions

import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.mindlog.R
import com.example.mindlog.core.dispatcher.DispatcherModule
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.adapter.JournalAdapter
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(DispatcherModule::class)
@RunWith(AndroidJUnit4::class)
class JournalIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var repository: TestJournalRepository

    @Before
    fun setup() {
        hiltRule.inject()
        repository.clear()
    }

    @Test
    fun journal_EndToEnd_UserJourney() {
        // 1. 홈 화면 진입
        val scenario = ActivityScenario.launch(HomeActivity::class.java)

        // 2. 일기 작성 버튼 클릭
        onView(withId(R.id.fabWrite)).perform(ViewActions.click())

        // 3. 감정 선택 화면 (EmotionSelectFragment)
        onView(allOf(withId(R.id.rb_center), isDescendantOfA(withId(R.id.row_sad_happy))))
            .perform(ViewActions.click())

        // 다음 화면으로 이동
        onView(withId(R.id.btn_next_or_save)).perform(ViewActions.click())

        // 4. 내용 작성 화면 (ContentWriteFragment)
        onView(withId(R.id.et_title)).perform(ViewActions.replaceText("통합 테스트 일기"))
        onView(withId(R.id.et_content)).perform(ViewActions.replaceText("통합 테스트 내용"))
        onView(withId(R.id.et_gratitude)).perform(ViewActions.replaceText("감사합니다"))

        // 저장 버튼 클릭
        onView(withId(R.id.btn_next_or_save)).perform(ViewActions.click())

        Thread.sleep(1000)

        onView(withText("통합 테스트 일기")).check(matches(isDisplayed()))

        onView(withId(R.id.rv_diary_feed))
            .perform(RecyclerViewActions.actionOnItemAtPosition<JournalAdapter.ViewHolder>(0,
                ViewActions.click()))

        // 상세 내용 확인 (TextView ID: tv_title, tv_content)
        onView(withId(R.id.tv_title)).check(matches(withText("통합 테스트 일기")))
        onView(withId(R.id.tv_content)).check(matches(withText("통합 테스트 내용")))

        // 7. 수정 화면 진입
        onView(withId(R.id.btn_detail_edit)).perform(ViewActions.click())
        // 수정 화면 툴바 제목 확인
        onView(withText("일기 수정")).check(matches(isDisplayed()))

        // 제목 수정
        onView(withId(R.id.et_title)).perform(ViewActions.replaceText("수정된 제목"))
        onView(withId(R.id.btn_edit_save)).perform(ViewActions.click())

        Thread.sleep(500)

        // 수정된 내용이 상세 화면에 반영되었는지 확인 (DetailActivity로 돌아옴)
        onView(withId(R.id.tv_title)).check(matches(withText("수정된 제목")))

        // 8. 삭제
        onView(withId(R.id.btn_detail_delete)).perform(ViewActions.click())

        onView(withText("삭제")).inRoot(isDialog()).perform(ViewActions.click())

        Thread.sleep(500)

        // 목록에서 사라졌는지 확인
        onView(withText("수정된 제목")).check(ViewAssertions.doesNotExist())

        scenario.close()
    }}
