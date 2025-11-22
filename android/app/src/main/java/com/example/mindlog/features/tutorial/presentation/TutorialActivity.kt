package com.example.mindlog.features.tutorial

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mindlog.R
import com.example.mindlog.databinding.ActivityTutorialBinding
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.domain.model.TutorialPage
import com.example.mindlog.features.tutorial.presentation.TutorialAdapter

class TutorialActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialBinding
    private lateinit var pagerAdapter: TutorialAdapter

    private val pages by lazy { generateTutorialPages() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupButtons()

        binding.tvSkip.bringToFront()
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

        return buildList {
            addAll(pagesFrom("tutorial_write", "일기 작성"))
            addAll(pagesFrom("tutorial_detail", "일기 상세 보기 및 수정"))
            addAll(pagesFrom("tutorial_search", "일기 검색"))
            addAll(pagesFrom("tutorial_selfaware", "나 알아가기"))
            addAll(pagesFrom("tutorial_statistic", "통계"))
            addAll(pagesFrom("tutorial_analysis", "분석"))
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
        binding.tvSkip.setOnClickListener { finishTutorial() }

        binding.btnNext.setOnClickListener {
            val current = binding.vpTutorial.currentItem
            if (current == pages.lastIndex) {
                finishTutorial()
            } else {
                binding.vpTutorial.currentItem = current + 1
            }
        }

        // 처음 상태
        updateButtonText(0)
        updatePageInfo(0)
        binding.progressIndicator.setProgressCompat(1, false)
    }

    private fun updateButtonText(position: Int) {
        binding.btnNext.text =
            if (position == pages.lastIndex) "시작하기" else "다음"
    }

    private fun updatePageInfo(position: Int) {
        val page = pages[position]
        binding.tvPageInfo.text = "${page.feature}  ${position + 1}/${pages.size}"
    }

    private fun finishTutorial() {
        // 튜토리얼 완료 플래그 저장
        val prefs = getSharedPreferences("tutorial_prefs", MODE_PRIVATE)
        prefs.edit().putBoolean("completed", true).apply()

        // 홈 화면으로 이동
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}