package com.example.mindlog.features.tutorial

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mindlog.databinding.ActivityTutorialMenuBinding
import dagger.hilt.android.AndroidEntryPoint
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.tutorial.domain.model.TutorialFeature
import com.example.mindlog.features.tutorial.presentation.TutorialFeatureAdapter

@AndroidEntryPoint
class TutorialMenuActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialMenuBinding
    private var isFinishScreen: Boolean = false

    private val returnToSettings: Boolean by lazy {
        intent.getBooleanExtra(TutorialActivity.EXTRA_RETURN_TO_SETTINGS, false)
    }

    // 기능 목록 정의 (TutorialActivity.generateTutorialPages() 의 feature 라벨과 일치해야 함)
    private val features = listOf(
        TutorialFeature("일기 작성", "일기를 새로 작성하는 방법을 안내합니다."),
        TutorialFeature("일기 상세 보기 및 수정", "기존 일기를 열어보고 수정하는 방법을 안내합니다."),
        TutorialFeature("일기 검색", "원하는 일기를 찾는 다양한 방법을 안내합니다."),
        TutorialFeature("질문으로 나 알아가기", "AI가 생성한 질문과 답변을 통해 자신을 알아가는 방법을 안내합니다."),
        TutorialFeature("일기 통계", "일기 데이터에 대한 다양한 통계를 확인하는 방법을 안내합니다."),
        TutorialFeature("나에 대한 분석", "질문/답변을 기반으로 생성된 나에 대한 분석을 보는 방법을 안내합니다."),
        TutorialFeature("설정", "다양한 설정을 변경하는 방법을 안내합니다."),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvTutorialMenu.layoutManager = GridLayoutManager(this, 2)

        val adapter = TutorialFeatureAdapter(features) { feature ->
            openTutorialFor(feature)
        }
        binding.rvTutorialMenu.adapter = adapter
        binding.btnFinishTutorialMenu.setOnClickListener {
            onFinishButtonClicked()
        }
    }

    private fun onFinishButtonClicked() {
        // 설정 화면에서 들어온 경우: 캐릭터 엔딩 화면 없이 바로 복귀
        if (returnToSettings) {
            finish()
            return
        }

        if (!isFinishScreen) {
            // 1️⃣ 첫 번째 클릭: 메뉴 → 엔딩 화면 전환
            isFinishScreen = true

            binding.groupMenuContent.visibility = View.GONE
            binding.finishContainer.visibility = View.VISIBLE
            binding.btnFinishTutorialMenu.text = "시작하기"  // 버튼 텍스트 변경
        } else {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun openTutorialFor(feature: TutorialFeature) {
        val intent = Intent(this, TutorialActivity::class.java).apply {
            putExtra(TutorialActivity.EXTRA_FEATURE_LABEL, feature.label)
        }
        startActivity(intent)
    }
}