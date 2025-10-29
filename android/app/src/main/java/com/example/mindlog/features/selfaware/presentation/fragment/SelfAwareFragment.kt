package com.example.mindlog.features.selfaware.presentation.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
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
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.view.inputmethod.BaseInputConnection

@AndroidEntryPoint
class SelfAwareFragment : Fragment(R.layout.fragment_self_aware) {
    private var _binding: FragmentSelfAwareBinding? = null
    private val binding get() = _binding!!
    private val vm: SelfAwareViewModel by viewModels()

    private var answerWatcher: TextWatcher? = null
    private var suppressAnswerTextChange = false
    private var forceOverlay: Boolean = false
    private var lastRadarCats: List<String>? = null
    private var lastRadarScores: List<Float>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareBinding.bind(view)
        binding.completionOverlay.bringToFront()

        answerWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                if (suppressAnswerTextChange) return
                if (isComposing(editable)) return
                vm.updateAnswerText(editable?.toString().orEmpty())
            }
        }
        binding.etAnswer.addTextChangedListener(answerWatcher)

        binding.btnSubmit.setOnClickListener {
            if (!binding.btnSubmit.isEnabled) return@setOnClickListener
            forceOverlay = true
            vm.submit()
        }

        binding.btnOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.selfAwareHistoryFragment)
        }

        // observe state
        // collect
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    val shouldShowOverlay = forceOverlay || s.isSubmitting || s.isAnsweredToday
                    binding.completionOverlay.isVisible = shouldShowOverlay
                    binding.groupQuestion.isVisible = !shouldShowOverlay

                    if (shouldShowOverlay) {
                        binding.completionOverlay.bringToFront()
                        binding.btnSubmit.isEnabled = false
                        binding.etAnswer.isEnabled = false
                        // 제출 확정/종료되면 오버레이 강제 플래그 해제
                        if (!s.isSubmitting) forceOverlay = false
                    } else {
                        val isQuestionVisible = !s.isAnsweredToday && !s.isSubmitting
                        binding.groupQuestion.isVisible = isQuestionVisible
                        binding.etAnswer.isEnabled = isQuestionVisible && !s.isLoadingQuestion
                        binding.tvQuestion.text = s.questionText ?: "오늘의 질문을 불러오고 있어요…"

                        val desired = s.answerText
                        val et = binding.etAnswer
                        val current = et.text?.toString().orEmpty()
                        val composing = isComposing(et.text)
                        if (!et.hasFocus() && !composing && current != desired) {
                            suppressAnswerTextChange = true
                            et.setText(desired)
                            et.setSelection(et.text?.length ?: 0)
                            suppressAnswerTextChange = false
                        }

                        binding.btnSubmit.isEnabled =
                            (s.questionId != null) && s.answerText.isNotBlank() && !s.isLoadingQuestion
                    }

                    // 카테고리/점수 안전 매핑 + 빈 차트 방어
                    val categories = s.valueCategories
                    val scores = s.categoryScores.map { (it.score ?: 0).toFloat() }
                    val chartEmpty = (binding.radar.data == null || binding.radar.data.dataSetCount == 0)
                    val needRender = chartEmpty || lastRadarCats != categories || lastRadarScores != scores

                    val visible = s.categoryScores.isNotEmpty()
                    if (needRender) {
                        renderRadar(binding.radar, categories, scores)
                        lastRadarCats = categories.toList()
                        lastRadarScores = scores.toList()
                    }
                    binding.tvValueSummary.text = "최근 답변을 바탕으로 산출된 가치 분포예요."

                    // chips
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

                    binding.tvPersonalityInsight.text =
                        if (s.personalityInsight.isNotBlank()) s.personalityInsight
                        else "성격 및 가치 분석 결과를 불러오는 중이에요..."
                    binding.tvComment.text =
                        if (s.comment.isNotBlank()) s.comment
                        else "AI 코멘트를 불러오는 중이에요..."
                }
            }
        }

        // initial load
        vm.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        lastRadarCats = null
        lastRadarScores = null
    }

    private fun isComposing(text: CharSequence?): Boolean {
        val sp = text as? Spannable ?: return false
        val start = BaseInputConnection.getComposingSpanStart(sp)
        val end = BaseInputConnection.getComposingSpanEnd(sp)
        return start != -1 && end != -1 && start != end
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

            setExtraOffsets(24f, 28f, 24f, 28f)

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