package com.example.mindlog.features.analysis.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentAnalysisBinding
import com.example.mindlog.features.analysis.domain.model.ComprehensiveAnalysis
import com.example.mindlog.features.analysis.domain.model.PersonalizedAdvice
import com.example.mindlog.features.analysis.domain.model.UserType
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalysisFragment : Fragment(R.layout.fragment_analysis), HomeActivity.FabClickListener {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: AnalysisViewModel by viewModels()

    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 작성 완료 시 홈의 Journal 탭으로 이동
                (activity as? HomeActivity)?.let { homeActivity ->
                    homeActivity.navigateToJournalTab()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    renderLoading(s.isLoading)
                    renderUserType(s.userType)
                    renderComprehensive(s.comprehensiveAnalysis)
                    renderAdvice(s.advice)
                }
            }
        }

        // 최초 로딩
        viewModel.load()
    }


    // 로딩 상태: 필요하면 ProgressBar 추가해서 연결하면 됨
    private fun renderLoading(isLoading: Boolean) {
        // 예시: 상단 카드들을 비/활성화
        binding.cardUserType.alpha = if (isLoading) 0.4f else 1f
        binding.cardComprehensive.alpha = if (isLoading) 0.4f else 1f
        binding.cardAdvice.alpha = if (isLoading) 0.4f else 1f
    }

    private fun renderUserType(userType: UserType?) {
        val card = binding.cardUserType
        card.isVisible = true

        if (userType == null) {
            binding.tvUserTypeName.text = "아직 분석이 준비되지 않았어요"
            binding.tvUserTypeDescription.text = "조금 더 기록이 쌓이면 유형 분석을 제공해드릴게요."
            binding.ivUserTypeCharacter.setImageResource(R.drawable.ic_user_type_undefined)
            return
        }

        val typeName = userType.userType
        val description = userType.description

        binding.tvUserTypeName.text = typeName
        binding.tvUserTypeDescription.text = description

        // 유형별 캐릭터 이미지 매핑
        val drawableRes = when (userType.userType) {
            "목표 지향형" -> R.drawable.ic_user_type_goal_oriented
            "탐험가형" -> R.drawable.ic_user_type_explorer
            "사교가형" -> R.drawable.ic_user_type_connector
            "배려형" -> R.drawable.ic_user_type_supporter
            "사색가형" -> R.drawable.ic_user_type_thinker
            "도전형" -> R.drawable.ic_user_type_challenger
            "안전추구형" -> R.drawable.ic_user_type_stability_seeker
            "감성형" -> R.drawable.ic_user_type_sensitive
            "분석형" -> R.drawable.ic_user_type_systematic
            "변화추구형" -> R.drawable.ic_user_type_reformer
            "균형형" -> R.drawable.ic_user_type_balanced
            else -> R.drawable.ic_user_type_undefined // 기본값
        }
        binding.ivUserTypeCharacter.setImageResource(drawableRes)
    }

    // 🔥 여기 완전히 새로 교체
    private fun renderComprehensive(analysis: ComprehensiveAnalysis?) {
        val card = binding.cardComprehensive
        card.isVisible = true

        val items: List<Pair<String, String>> =
            if (analysis == null) {
                listOf(
                    "아직 분석이 준비되지 않았어요" to
                            "자기 인식 질문과 일기를 조금 더 기록해주시면, Five Factor 기반 심층 분석을 카드 형식으로 보여드릴게요.",
                )
            } else {
                listOf(
                    "성실성 (Conscientiousness)" to analysis.conscientiousness,
                    "정서 안정성 (Neuroticism)" to analysis.neuroticism,
                    "외향성 (Extraversion)" to analysis.extraversion,
                    "개방성 (Openness)" to analysis.openness,
                    "수용성 (Agreeableness)" to analysis.agreeableness,
                )
            }

        val useColorfulBackground = analysis != null
        val adapter = FiveFactorAdapter(items, useColorfulBackground)
        binding.vpFiveFactor.adapter = adapter
        binding.dotsIndicator.attachTo(binding.vpFiveFactor)
    }

    private fun renderAdvice(advice: PersonalizedAdvice?) {
        val card = binding.cardAdvice
        card.isVisible = true

        if (advice == null) {
            binding.tvAdviceType.text = "아직 개인화 조언이 없어요"
            binding.tvAdviceBody.text = "기록이 조금 더 쌓이면 맞춤형 조언을 드릴게요."
            binding.tvAdviceTypeDescription.isVisible = false
            return
        }

        // 예시: emoji + title + body 구조라고 가정
        binding.tvAdviceType.text = "조언 유형: " + advice.adviceType
        binding.tvAdviceBody.text = advice.personalizedAdvice

        binding.tvAdviceTypeDescription.isVisible = true
        val typeDescription = when (advice.adviceType) {
            "EQ" -> "EQ(Emotional Intelligence Quotient)는 감정을 인식·이해·조절하고 타인의 감정에 공감하는 능력, 관계 유지와 의사결정의 질을 높이기 위한 조언입니다."
            "CBT" -> "CBT(Cognitive Behavioral Therapy)는 비합리적 사고 패턴을 인식해 재구성하고, 행동 실험을 통해 현실적이고 도움이 되는 사고·행동으로 교체하기 위한 조언입니다."
            "ACT" -> "ACT(Acceptance and Commitment Therapy)는 불편한 감정을 억누르기보다 받아들이고, 개인의 핵심가치에 기반한 행동을 선택하도록 돕는 수용·헌신 중심의 조언입니다."
            else -> ""
        }
        binding.tvAdviceTypeDescription.text = typeDescription
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}