package com.example.mindlog.features.selfaware.presentation.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
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
import android.text.Editable
import android.text.Spannable
import android.text.TextWatcher
import android.view.inputmethod.BaseInputConnection

@AndroidEntryPoint
class SelfAwareFragment : Fragment(R.layout.fragment_self_aware) {
    private var _binding: FragmentSelfAwareBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SelfAwareViewModel by viewModels()

    private var suppressAnswerTextChange = false
    private var forceOverlay: Boolean = false
    private var lastRadarCats: List<String>? = null
    private var lastRadarScores: List<Float>? = null

    private fun showOverlay(force: Boolean) {
        if (force) forceOverlay = true
        val overlay = binding.completionOverlay
        overlay.visibility = View.VISIBLE
        overlay.alpha = 1f
        ViewCompat.setElevation(overlay, 10000f)
        overlay.bringToFront()

        // Ensure it covers the parent fully even if XML had wrap/0dp issues
        overlay.layoutParams = overlay.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
        overlay.isClickable = true
        overlay.isFocusable = true

        // Hide/disable question UI while overlay is up
        binding.groupQuestion.isVisible = false
        binding.etAnswer.isEnabled = false
        binding.btnSubmit.isEnabled = false
    }

    private fun hideOverlay() {
        forceOverlay = false
        val overlay = binding.completionOverlay
        overlay.visibility = View.GONE
        overlay.isClickable = false
        overlay.isFocusable = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareBinding.bind(view)

        binding.completionOverlay.bringToFront()

        ViewCompat.setElevation(binding.completionOverlay, 10000f)
        binding.completionOverlay.layoutParams = binding.completionOverlay.layoutParams.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        binding.etAnswer.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable?) {
                if (suppressAnswerTextChange) return
                if (isComposing(editable)) return
                viewModel.updateAnswerText(editable?.toString().orEmpty())
            }
        })

        binding.btnSubmit.setOnClickListener {
            if (!binding.btnSubmit.isEnabled) return@setOnClickListener
            showOverlay(force = true)
            viewModel.submit()
        }

        binding.btnOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.selfAwareHistoryFragment)
        }

        // observe state
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->

                    // 0) 제출 흐름: 오버레이 표시 우선 규칙
                    val shouldShowOverlay = forceOverlay || s.isSubmitting || s.isAnsweredToday
                    if (shouldShowOverlay) {
                        showOverlay(force = false)
                    } else {
                        hideOverlay()

                        // 1) 질문 섹션 표시/로딩 (오버레이가 없을 때만 제어)
                        val isQuestionVisible = !s.isAnsweredToday && !s.isSubmitting
                        binding.groupQuestion.isVisible = isQuestionVisible
                        binding.etAnswer.isEnabled = isQuestionVisible && !s.isLoadingQuestion

                        // 2) 질문 텍스트
                        binding.tvQuestion.text = s.questionText ?: "오늘의 질문을 불러오고 있어요…"

                        // 3) EditText 내용 동기화 (IME 합성 중/포커스 중엔 되도록 건드리지 않는 게 좋음)
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

                        // 4) 제출 버튼 활성화 조건 (오버레이가 없을 때만 계산)
                        binding.btnSubmit.isEnabled =
                            (s.questionText != null) && s.answerText.isNotBlank() && !s.isLoadingQuestion && !s.isSubmitting
                    }

                    // 가치 레이더 차트 렌더링
                    if (s.categoryScores.isNotEmpty() && s.valueCatogories.size == s.categoryScores.size) {
                        val scores = s.categoryScores.map { it.score.toFloat() }
                        val cats = s.valueCatogories
                        val needRender = lastRadarCats != cats || lastRadarScores != scores

                        binding.cardValueMap.isVisible = true
                        if (needRender) {
                            renderRadar(binding.radar, cats, scores)
                            lastRadarCats = cats.toList()
                            lastRadarScores = scores.toList()
                        }
                        binding.tvValueSummary.text = "최근 답변을 바탕으로 산출된 가치 분포예요."
                    } else {
                        binding.cardValueMap.isVisible = false
                        lastRadarCats = null
                        lastRadarScores = null
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
                        if (s.personalityInsight.isNotBlank()) s.personalityInsight else "성격 및 가치 분석 결과를 불러오는 중이에요..."
                    binding.tvComment.text =
                        if (s.comment.isNotBlank()) s.comment else "AI 코멘트를 불러오는 중이에요..."
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