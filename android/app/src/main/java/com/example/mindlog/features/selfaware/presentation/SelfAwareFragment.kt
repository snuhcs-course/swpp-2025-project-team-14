package com.example.mindlog.features.selfaware.presentation

import android.content.Intent
import android.app.Activity
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity

@AndroidEntryPoint
class SelfAwareFragment : Fragment(R.layout.fragment_self_aware), HomeActivity.FabClickListener {
    private var _binding: FragmentSelfAwareBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val vm: SelfAwareViewModel by viewModels()

    private var answerWatcher: TextWatcher? = null
    private var suppressAnswerTextChange = false
    private var radarInitDone = false
    private var lastRadarCats: List<String>? = null
    private var lastRadarScores: List<Float>? = null
    private var valueMapLoadedOnce = false
    private var wasValueMapLoading = false

    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSelfAwareBinding.bind(view)
        binding.completionOverlay.bringToFront()

        // Journal 작성 화면에서 돌아올 때 결과를 처리하기 위한 launcher 설정
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 작성 완료 시 홈의 Journal 탭으로 이동
                (activity as? HomeActivity)?.navigateToJournalTab()
            }
        }

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
            vm.submit()
        }

        binding.btnOpenHistory.setOnClickListener {
            findNavController().navigate(R.id.selfAwareHistoryFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    // Show spinner if loading question, submitting, or loading value map (if present)
                    val isLoading = s.isLoading
                    val isQuestionError = s.isQuestionError
                    binding.progressValueMap.isVisible = isLoading

                    if (isLoading) {
                        wasValueMapLoading = true
                    }
                    if (!isLoading && wasValueMapLoading) {
                        valueMapLoadedOnce = true
                    }                    // valueMap 로딩 상태 추적: 최초 로딩이 끝난 이후에만 empty 상태를 보여주기 위함
                    if (isLoading) {
                        wasValueMapLoading = true
                    }
                    if (!isLoading && wasValueMapLoading) {
                        valueMapLoadedOnce = true
                    }


                    val shouldShowOverlay = s.showCompletionOverlay || s.isSubmitting || s.isAnsweredToday
                    val showQuestionLoading = (s.isLoadingQuestion || isQuestionError) && !shouldShowOverlay
                    binding.groupQuestionLoading.isVisible = showQuestionLoading
                    binding.completionOverlay.isVisible = shouldShowOverlay

                    if (shouldShowOverlay) {
                        binding.completionOverlay.bringToFront()
                        binding.groupQuestion.isVisible = false
                        binding.groupQuestionLoading.isVisible = false
                        binding.btnSubmit.isEnabled = false
                        binding.etAnswer.isEnabled = false
                    } else {
                        val isQuestionVisible =
                            !s.isAnsweredToday && !s.isSubmitting && !s.isLoadingQuestion && !isQuestionError

                        binding.groupQuestion.isVisible = isQuestionVisible
                        binding.groupQuestionLoading.isVisible = s.isLoadingQuestion || isQuestionError
                        binding.etAnswer.isEnabled = isQuestionVisible && !s.isLoadingQuestion && !isQuestionError

                        if (isQuestionError) {
                            binding.progressQuestion.isVisible = false
                            binding.ivQuestionError.isVisible = true
                            binding.tvQuestionLoading.text =
                                s.questionErrorMessage ?: "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요."
                        } else if (s.isLoadingQuestion) {
                            binding.progressQuestion.isVisible = true
                            binding.ivQuestionError.isVisible = false
                            binding.tvQuestionLoading.text = "오늘의 질문을 생성하는 중이에요…"
                        } else {
                            // 질문도 있고, 로딩/에러도 아님
                            binding.progressQuestion.isVisible = false
                            binding.ivQuestionError.isVisible = false
                        }

                        // 질문 텍스트: 질문이 있을 때만 의미 있음
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

                    // 카테고리/점수 안전 매핑 + 빈 차트/플레이스홀더 처리
                    val categories = s.valueMap.map { it.categoryKo }
                    val scores = s.valueMap.map { (it.score ?: 0).toFloat() }
                    val hasValueMap = categories.isNotEmpty() && scores.isNotEmpty()

                    val chartEmpty = binding.radar.data == null || binding.radar.data.dataSetCount == 0
                    val needRender = hasValueMap && (chartEmpty || lastRadarCats != categories || lastRadarScores != scores)
                    val showEmptyValueMap = valueMapLoadedOnce && !isLoading && !hasValueMap

                    when {
                        isLoading -> {
                            // 로딩 중: 차트/empty 둘 다 숨기고, 프로그레스만
                            binding.radar.isVisible = false
                            binding.lottieSelfAwareEmpty.isVisible = false
                        }

                        hasValueMap -> {
                            if (needRender) {
                                renderRadar(binding.radar, categories, scores)
                                lastRadarCats = categories.toList()
                                lastRadarScores = scores.toList()
                            }
                            binding.radar.isVisible = true
                            binding.lottieSelfAwareEmpty.isVisible = false
                            binding.tvValueSummary.text = "최근 답변을 바탕으로 산출된 가치 분포예요."
                        }
                        showEmptyValueMap -> {
                            // 로딩이 한 번 이상 끝났고, 데이터가 실제로 없을 때만 empty Lottie 노출
                            binding.radar.clear()
                            binding.radar.isVisible = false
                            binding.lottieSelfAwareEmpty.isVisible = true
                            binding.tvValueSummary.text =
                                "자기 가치 지도가 생성되지 않았어요. 스스로를 알아가는 질문에 답변해 보세요!"
                        }
                        else -> {
                            // 초기 상태 등: 아무것도 보여주지 않음 (깜빡임 방지)
                            binding.radar.isVisible = false
                            binding.lottieSelfAwareEmpty.isVisible = false
                        }
                    }

                    // 핵심 가치 키워드가 하나도 없으면 안내 문구 및 칩 숨김
                    val hasTopValues = s.topValueScores.any { !it.value.isNullOrBlank() }
                    if (!hasTopValues) {
                        binding.tvTopValueScoresSummary.text = "핵심 가치 키워드가 생성되지 않았어요. 스스로를 알아가는 질문에 답변해 보세요!"
                        binding.chipGroupValuesContainer.isVisible = false
                    } else {
                        binding.tvTopValueScoresSummary.text = "사용자님이 중시하는 가치들은 위와 같아요."
                        binding.chipGroupValuesContainer.isVisible = true
                    }

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

        // 1) 데이터 생성
        val entries = scores.map { RadarEntry(it) }
        val set = RadarDataSet(entries, "").apply {
            color = "#64B5F6".toColorInt()
            lineWidth = 2f
            setDrawValues(false)
            setDrawHighlightIndicators(false)
            setDrawFilled(true)
            fillColor = "#64B5F6".toColorInt()
            fillAlpha = 85
        }

        // 2) 이전 상태 정리 후 데이터 교체
        chart.clear()
        chart.data = RadarData(set)
        chart.setExtraOffsets(24f, 28f, 24f, 28f)

        // 3) 스타일/축 설정 (기존 그대로)
        chart.apply {
            setBackgroundColor(Color.TRANSPARENT)
            setDrawMarkers(false)
            description.isEnabled = false
            legend.isEnabled = false

            webColor = "#E3E5EC".toColorInt()
            webColorInner = "#A5A5A5".toColorInt()
            webLineWidth = 1.2f
            webLineWidthInner = 1f
            webAlpha = 180

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(categories)
                textColor = "#636779".toColorInt()
                textSize = 12f
                yOffset = 12f
                xOffset = 4f
                position = XAxis.XAxisPosition.TOP
            }

            yAxis.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                setLabelCount(5, true)
                setDrawLabels(false)
            }

            setTouchEnabled(false)
        }

        // 4) 첫 렌더는 애니메이션 없이 정확히 그린 뒤, 다음 프레임에 애니메이션
        chart.notifyDataSetChanged()
        chart.invalidate()

        if (!radarInitDone) {
            radarInitDone = true
            chart.post { chart.animateXY(500, 500) }   // 레이아웃 이후 애니메이션
        } else {
            chart.animateXY(500, 500)
        }
    }

}