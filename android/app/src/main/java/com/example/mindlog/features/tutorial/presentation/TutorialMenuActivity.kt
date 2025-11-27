package com.example.mindlog.features.tutorial

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mindlog.databinding.ActivityTutorialMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.domain.model.TutorialFeature
import com.example.mindlog.features.tutorial.presentation.TutorialFeatureAdapter

@AndroidEntryPoint
class TutorialMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialMenuBinding

    private val returnToSettings: Boolean by lazy {
        intent.getBooleanExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, false)
    }

    // 기능 목록 정의 (TutorialActivity.generateTutorialPages() 의 feature 라벨과 일치해야 함)
    private val features = listOf(
        TutorialFeature("일기 작성", "일기를 새로 작성하는 방법을 안내합니다."),
        TutorialFeature("일기 상세 보기 및 수정", "기존 일기를 열어보고 수정하는 방법을 안내합니다."),
        TutorialFeature("일기 검색", "원하는 일기를 빠르게 찾는 방법을 안내합니다."),
        TutorialFeature("나 알아가기", "나 알아가기 탭의 질문/답변 흐름을 안내합니다."),
        TutorialFeature("통계", "감정/가치 통계를 확인하는 방법을 안내합니다."),
        TutorialFeature("분석", "분석 탭에서 인사이트를 보는 방법을 안내합니다."),
        TutorialFeature("튜토리얼 완료", "기타 도움말/마무리 안내를 확인합니다."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = TutorialFeatureAdapter(features) { feature ->
            openTutorialFor(feature)
        }
        binding.rvTutorialMenu.adapter = adapter
        binding.btnFinishTutorialMenu.setOnClickListener {
            if (returnToSettings) {
                finish()
            } else {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (returnToSettings) {
            // 설정에서 들어온 경우: 단순 종료
            finish()
        } else {
            // 온보딩에서 들어온 경우: 홈 화면 이동
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun openTutorialFor(feature: TutorialFeature) {
        val intent = Intent(this, TutorialActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_FEATURE_LABEL, feature.label)
            // 설정 화면에서 열린 메뉴라면, 필요 시 true로 넘겨도 됨
            // putExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, true)
        }
        startActivity(intent)
    }
}