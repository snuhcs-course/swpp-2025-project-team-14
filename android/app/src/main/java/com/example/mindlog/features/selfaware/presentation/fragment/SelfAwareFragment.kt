package com.example.mindlog.features.selfaware.presentation.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentSelfAwareBinding
import com.example.mindlog.features.selfaware.presentation.viewmodel.SelfAwareViewModel
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SelfAwareFragment : Fragment(R.layout.fragment_self_aware) {
    private var _binding: FragmentSelfAwareBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SelfAwareViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareBinding.bind(view)

        binding.etAnswer.doAfterTextChanged {
            viewModel.updateAnswerText(it?.toString().orEmpty())
        }

        binding.btnSubmit.setOnClickListener {
            if (!binding.btnSubmit.isEnabled) return@setOnClickListener
            // 즉시 반응
            binding.btnSubmit.isEnabled = false
            binding.completionOverlay.visibility = View.VISIBLE
            viewModel.submit()
        }

        binding.btnOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.selfAwareHistoryFragment)
        }

        // observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    binding.btnSubmit.isEnabled = !s.isLoading && !s.isSubmitting

                    // 질문 입력/완료 토글
                    binding.groupQuestion.isVisible = !s.isAnsweredToday
                    binding.tvQuestion.text = s.questionText ?: "AI가 오늘의 질문을 생성 중이에요…"
                    binding.btnSubmit.isEnabled = s.questionText != null && s.answerText.isNotBlank()

                    // 가치 점수 UI 갱신 (예: 막대/레이더 등)
                    // 에디트텍스트와 상태 동기화(무한 루프 방지: 값이 다를 때만 반영)
                    val current = binding.etAnswer.text?.toString().orEmpty()
                    if (current != s.answerText) {
                        binding.etAnswer.setText(s.answerText)
                        binding.etAnswer.setSelection(binding.etAnswer.text?.length ?: 0)
                    }

                    // 완료 상태 UI
                    binding.groupQuestion.isVisible = !s.isAnsweredToday
                    // 답변 완료 시 오버레이 한 번만 표시
                    if (s.isAnsweredToday && binding.completionOverlay.visibility != View.VISIBLE) {
                        // 입력 비활성화 및 축하 오버레이 표시
                        binding.etAnswer.isEnabled = false
                        binding.btnSubmit.isEnabled = false
                        showCompletionOverlay()
                    }

                    // 가치 레이더 차트 렌더링
                    if (s.categoryScores.isNotEmpty() && s.valueCatogories.size == s.categoryScores.size) {
                        val scores = s.categoryScores.map { it.score.toFloat() }
                        binding.cardValueMap.isVisible = true
                        renderRadar(binding.radar, s.valueCatogories, scores)
                        binding.tvValueSummary.text = "최근 답변을 바탕으로 산출된 가치 분포예요."
                    } else {
                        // 값이 없으면 카드 숨김(또는 플레이스홀더 유지)
                        binding.cardValueMap.isVisible = false
                    }

                    // 상위 가치 키워드
                    val chipGroup = binding.chipGroupValues
                    val chips = listOf(
                        binding.chipValueFirst,
                        binding.chipValueSecond,
                        binding.chipValueThird,
                        binding.chipValueFourth,
                        binding.chipValueFifth
                    )
                    for (i in chips.indices) {
                        val chip = chips[i]
                        val value = s.topValueScores.getOrNull(i)?.value ?: ""
                        chip.text = value
                        chip.isVisible = value.isNotBlank()
                    }

                    // 성격 및 인사이트 업데이트
                    binding.tvPersonalityInsight.text =
                        if (s.personalityInsight.isNotBlank()) s.personalityInsight else "성격 및 가치 분석 결과를 불러오는 중입니다."
                    binding.tvComment.text =
                        if (s.comment.isNotBlank()) s.comment else "AI 코멘트를 불러오는 중입니다."
                }
            }
        }

        // initial load
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showCompletionOverlay() {
        binding.completionOverlay.visibility = View.VISIBLE
    }

    private fun renderRadar(
        chart: RadarChart,
        categories: List<String>,
        scores: List<Float>,
    ) {
        if (categories.isEmpty() || scores.isEmpty() || categories.size != scores.size) {
            chart.clear(); return
        }

        val entries = scores.map { RadarEntry(it ) }

        val set = RadarDataSet(entries, "").apply {
            // 외각선 설정
            color = Color.parseColor("#64B5F6")
            lineWidth = 2f
            setDrawValues(false)
            setDrawHighlightIndicators(false)
            // 내부 설정
            setDrawFilled(true)
            fillColor = Color.parseColor("#64B5F6")
            fillAlpha = 85                                 // 살짝 투명
        }

        chart.apply {
            data = RadarData(set)

            // 배경/웹(거미줄) 스타일
            setBackgroundColor(Color.TRANSPARENT)
            setDrawMarkers(false)
            description.isEnabled = false
            legend.isEnabled = false

            webColor = Color.parseColor("#E3E5EC")        // 바깥선
            webColorInner = Color.parseColor("#A5A5A5")   // 안쪽선
            webLineWidth = 1.2f
            webLineWidthInner = 1f
            webAlpha = 180

            // X축 = 카테고리 라벨
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(categories)
                textColor = Color.parseColor("#636779")   // 라벨 색
                textSize = 12f
                yOffset = 12f
                xOffset = 4f
                position = XAxis.XAxisPosition.TOP
            }

            // Y축 = 값(숫자) 라벨은 숨김
            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(5, true)
                setDrawLabels(false)
            }

            // 터치/애니메이션
            setTouchEnabled(false)
            animateXY(500, 500)

            invalidate()
        }
    }

}