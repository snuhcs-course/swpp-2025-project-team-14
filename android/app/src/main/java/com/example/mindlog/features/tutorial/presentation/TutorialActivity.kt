package com.example.mindlog.features.tutorial

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mindlog.R
import com.example.mindlog.databinding.ActivityTutorialBinding
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.domain.model.TutorialPage
import com.example.mindlog.features.tutorial.presentation.TutorialAdapter
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TutorialActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RETURN_TO_SETTINGS = "extra_return_to_settings"
        const val EXTRA_FEATURE_LABEL = "extra_feature_label"   // 어떤 기능 튜토리얼인지 (예: "일기 작성")
        const val EXTRA_GO_TO_MENU = "extra_go_to_menu"
    }

    private lateinit var binding: ActivityTutorialBinding
    private lateinit var pagerAdapter: TutorialAdapter

    private val allPages by lazy { generateTutorialPages() }

    // 메뉴 Activity에서 전달된 기능 이름 (예: "일기 작성", "통계")
    private val selectedFeatureLabel: String? by lazy {
        intent.getStringExtra(EXTRA_FEATURE_LABEL)
    }

    // 실제 ViewPager에 표시할 페이지: 기능이 지정되어 있으면 해당 기능만 필터링, 아니면 전체
    private val pages: List<TutorialPage> by lazy {
        val feature = selectedFeatureLabel
        if (feature.isNullOrEmpty()) {
            allPages
        } else {
            allPages.filter { it.feature == feature }
                // 혹시 필터 결과가 비어 있으면 안전하게 전체를 보여주도록 fallback
                .ifEmpty { allPages }
        }
    }

    private val goToMenu: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_GO_TO_MENU, false)
    }

    private val returnToSettings: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_RETURN_TO_SETTINGS, false)
    }

    private val isFeatureTutorial: Boolean by lazy {
        !selectedFeatureLabel.isNullOrEmpty()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()

        binding.btnSkip.bringToFront()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_FULLSCREEN

        actionBar?.hide()
    }

    private fun getDrawableIds(prefix: String): List<Int> {
        val fields = R.drawable::class.java.fields
        return fields
            .filter { it.name.startsWith(prefix) }
            .sortedBy { it.name.removePrefix(prefix).toInt() }
            .map { it.get(null) as Int }
    }

    private fun getDrawablesByPrefix(prefix: String): List<Int> {
        return R.drawable::class.java.fields
            .mapNotNull { field ->
                if (field.name.startsWith(prefix)) {
                    val index = field.name.removePrefix(prefix).toIntOrNull()
                    if (index != null) index to (field.get(null) as Int) else null
                } else {
                    null
                }
            }
            .sortedBy { it.first }
            .map { it.second }
    }

    private fun generateTutorialPages(): List<TutorialPage> {
        fun pagesFrom(prefix: String, feature: String): List<TutorialPage> =
            getDrawablesByPrefix(prefix).map { resId ->
                TutorialPage(imageRes = resId, feature = feature)
            }

        if (goToMenu) {
            val introPages = getDrawableIds("tutorial_first").map { resId ->
                TutorialPage(imageRes = resId, feature = "앱 소개")
            }
            if (introPages.isNotEmpty()) {
                return introPages
            }
        }

        return buildList {
            addAll(pagesFrom("tutorial_write", "일기 작성"))
            addAll(pagesFrom("tutorial_detail", "일기 상세 보기 및 수정"))
            addAll(pagesFrom("tutorial_search", "일기 검색"))
            addAll(pagesFrom("tutorial_selfaware", "질문으로 나 알아가기"))
            addAll(pagesFrom("tutorial_statistic", "일기 통계"))
            addAll(pagesFrom("tutorial_analysis", "나의 심리 성향 분석"))
            addAll(pagesFrom("tutorial_setting", "설정"))
        }
    }

    private fun setupViewPager() {
        pagerAdapter = TutorialAdapter(pages)
        binding.vpTutorial.adapter = pagerAdapter
        binding.vpTutorial.orientation = ViewPager2.ORIENTATION_HORIZONTAL

        // progress max를 전체 페이지 수로 설정 (position+1로 채울 거라 맥스 = pages.size)
        binding.progressIndicator.max = pages.size

        binding.vpTutorial.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateButtonText(position)
                updatePageInfo(position)

                // 진행도 업데이트 (1 ~ pages.size)
                binding.progressIndicator.setProgressCompat(position + 1, /*animated=*/true)
            }
        })
    }

    private fun setupButtons() {
        binding.btnSkip.setOnClickListener { finishTutorial() }

        binding.btnNext.setOnClickListener {
            val current = binding.vpTutorial.currentItem
            if (current == pages.lastIndex) {
                finishTutorial()
            } else {
                binding.vpTutorial.currentItem = current + 1
            }
        }

        if (goToMenu) {
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnSkip.visibility = View.VISIBLE
        }

        // 페이지가 하나도 없으면 더 이상 진행하지 않고 안전하게 종료
        if (pages.isEmpty()) {
            finish()
            return
        }

        // 처음 상태
        updateButtonText(0)
        updatePageInfo(0)
        binding.progressIndicator.setProgressCompat(1, false)
    }

    private fun updateButtonText(position: Int) {
        binding.btnNext.text =
            if (position == pages.lastIndex) {
                when {
                    goToMenu -> "튜토리얼 메뉴로 가기"
                    isFeatureTutorial -> "메뉴로 돌아가기"
                    else -> "시작하기"
                }
            } else {
                "다음"
            }
    }

    private fun updatePageInfo(position: Int) {
        if (pages.isEmpty()) return
        val page = pages[position]
        binding.tvPageInfo.text = "${page.feature}  ${position + 1}/${pages.size}"
    }

    private fun finishTutorial() {
        // 튜토리얼 완료 플래그 저장
        val prefs = getSharedPreferences("tutorial_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("completed", true).commit()

        when {
            // Main/Home 에서 인트로용으로 들어온 경우: 메뉴로 이동
            goToMenu -> {
                startActivity(Intent(this, TutorialMenuActivity::class.java).apply {
                    putExtra(EXTRA_RETURN_TO_SETTINGS, returnToSettings)
                })
                finish()
            }

            // 설정 화면에서 온 경우: 원래 화면으로 돌아가기
            returnToSettings -> {
                finish()
            }

            // 튜토리얼 메뉴에서 특정 기능을 골라 들어온 경우: 뒤로 돌아가면 메뉴로 복귀
            isFeatureTutorial -> {
                finish()
            }

            // 그 외 기존 플로우: 튜토리얼 끝나면 홈 화면으로 이동
            else -> {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

    fun completeTutorialForTest() {
        finishTutorial()
    }
}