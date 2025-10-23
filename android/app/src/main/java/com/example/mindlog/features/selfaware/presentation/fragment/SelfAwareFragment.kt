package com.example.mindlog.features.selfaware.presentation.fragment

import android.graphics.Color
import android.os.Bundle
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

        // initial load
        viewModel.load()

        binding.etAnswer.doAfterTextChanged {
            viewModel.updateAnswerText(it?.toString().orEmpty())
        }

        binding.btnSubmit.setOnClickListener { viewModel.submit() }

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
                    binding.tvQuestion.text = s.questionText ?: "질문을 불러오는 중…"
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
                    binding.btnSubmit.text = if (s.isAnsweredToday) "오늘 답변 완료" else "답변 완료"
                    binding.btnSubmit.isEnabled = !s.isAnsweredToday && s.questionText != null && s.answerText.isNotBlank()

                    // 가치 레이더 차트 렌더링
                    if (s.valueScores.isNotEmpty() && s.valueLabels.isNotEmpty() && s.valueLabels.size == s.valueScores.size) {
                        binding.cardValueMap.isVisible = true
                        renderRadar(binding.radar, s.valueLabels, s.valueScores)
                        binding.tvValueSummary.text = "최근 답변을 바탕으로 산출된 가치 분포예요."
                    } else {
                        // 값이 없으면 카드 숨김(또는 플레이스홀더 유지)
                        binding.cardValueMap.isVisible = false
                    }

                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun renderRadar(
        chart: RadarChart,
        labels: List<String>,
        values: List<Float>,
    ) {
        if (labels.isEmpty() || values.isEmpty() || labels.size != values.size) {
            chart.clear(); return
        }

        val entries = values.map { RadarEntry(it ) }

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
                valueFormatter = IndexAxisValueFormatter(labels)
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