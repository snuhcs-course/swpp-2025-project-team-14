package com.example.mindlog.features.analysis.presentation

import android.os.Bundle
import android.view.View
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AnalysisViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

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

    private fun renderComprehensive(analysis: ComprehensiveAnalysis?) {
        val card = binding.cardComprehensive
        card.isVisible = true
        if (analysis == null) {
            binding.tvConscientiousness.text = "아직 데이터가 부족해요."
            binding.tvNeuroticism.text = "조금 더 기록해주시면 분석을 제공할게요."
            binding.tvExtraversion.text = "아직 데이터가 부족해요."
            binding.tvOpenness.text = "조금 더 기록해주시면 분석을 제공할게요."
            binding.tvAgreeableness.text = "아직 데이터가 부족해요."
            return
        }

        binding.tvConscientiousness.text = analysis.conscientiousness
        binding.tvNeuroticism.text = analysis.neuroticism
        binding.tvExtraversion.text = analysis.extraversion
        binding.tvOpenness.text = analysis.openness
        binding.tvAgreeableness.text = analysis.agreeableness
    }

    private fun renderAdvice(advice: PersonalizedAdvice?) {
        val card = binding.cardAdvice
        card.isVisible = true
        if (advice == null) {
            binding.tvAdviceEmoji.text = "✨"
            binding.tvAdviceType.text = "아직 개인화 조언이 없어요"
            binding.tvAdviceBody.text = "기록이 조금 더 쌓이면 맞춤형 조언을 드릴게요."
            return
        }

        // 예시: emoji + title + body 구조라고 가정
        binding.tvAdviceEmoji.text = advice.adviceType ?: "✨"
        binding.tvAdviceType.text = advice.adviceType
        binding.tvAdviceBody.text = advice.personalizedAdvice
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}