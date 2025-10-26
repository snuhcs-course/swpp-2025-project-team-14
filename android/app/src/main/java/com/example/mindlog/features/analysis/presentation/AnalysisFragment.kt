package com.example.mindlog.features.analysis.presentation

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentAnalysisBinding
import com.google.android.material.textview.MaterialTextView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalysisViewModel by viewModels()
    private lateinit var valueScoreAdapter: ValueScoreAdapter


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        setupValueList()
        setupClicks()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    // 기간 표시
                    binding.tvPeriodRange.text = s.periodLabel

                    val checkedId = when (s.selectedEmotion) {
                        "행복" -> R.id.chipHappy
                        "슬픔" -> R.id.chipSad
                        "분노" -> R.id.chipAngry
                        "불안" -> R.id.chipAnxious
                        "평온" -> R.id.chipCalm
                        else -> View.NO_ID
                    }
                    binding.chipGroupEmotions.check(checkedId)

                    // Top-5 행복 이유 (더미 텍스트)
                    val events = s.EmotionEvents
                    // 보이는 라인 수만큼 채우기
                    val labels = listOf(
                        "최근 행복했던 이유는?",
                        events.getOrNull(0) ?: "",
                        events.getOrNull(1) ?: "",
                        events.getOrNull(2) ?: "",
                        events.getOrNull(3) ?: "",
                        events.getOrNull(4) ?: ""
                    )

                    // cardEmotionEvents 내부 TextView는 순서대로 찾아서 바인딩 (간단하게 id 없이 인덱스로)
                    // 첫 줄 타이틀은 이미 레이아웃에 있으니 5개만 업데이트:
                    // 여기선 안전하게 cardEmotionEvents 안의 LinearLayout 자식들을 탐색
                    val container = binding.cardEmotionEventsContainer // see binding alias below
                    val children = (0 until container.childCount).map { container.getChildAt(it) }
                    // update title
                    val titleView = children.getOrNull(0) as? MaterialTextView
                    val titleText = titleForEmotion(viewModel.state.value.selectedEmotion)
                    titleView?.text = titleText

                    // update 5 emotion events
                    for (i in 1..5) {
                        val tv =
                            children.getOrNull(i) as? com.google.android.material.textview.MaterialTextView
                        tv?.text = if (i <= events.size) "${i}. ${events[i - 1]}" else ""
                        tv?.isVisible = i <= events.size
                    }

                    // update value score graph
                    valueScoreAdapter.submitList(s.valueScores)

                    // update AI insight & comment
                    binding.tvAIInsight.text = s.aiInsight
                    binding.tvAIComment.text = s.aiComment
                }
            }
        }
    }

    private fun setupClicks() = binding.apply {
        btnWeekly.setOnClickListener { viewModel.setPeriod(PeriodPreset.WEEK) }
        btnMonthly.setOnClickListener { viewModel.setPeriod(PeriodPreset.MONTH) }
        btnCustom.setOnClickListener { viewModel.setPeriod(PeriodPreset.CUSTOM) }

        chipGroupEmotions.setOnCheckedStateChangeListener { group, ids ->
            val selected = ids.firstOrNull()?.let { id ->
                group.findViewById<com.google.android.material.chip.Chip>(id)?.text?.toString()
            }
            viewModel.setEmotionFilter(selected)
        }
    }

    private fun setupValueList() = binding.rvValueScores.apply {
        layoutManager = LinearLayoutManager(requireContext())
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = if (parent.getChildAdapterPosition(v) == 0) 0 else 10.dp
            }
        })
        valueScoreAdapter = ValueScoreAdapter { pos ->
            // 행별 색상
            val palette = listOf(
                "#FF6B6B", // 1. 열정/에너지 (붉은빛)
                "#FFA94D", // 2. 성취/도전 (주황)
                "#FFD43B", // 3. 즐거움/밝음 (노랑)
                "#69DB7C", // 4. 안정/균형 (초록)
                "#4DABF7", // 5. 신뢰/자기인식 (파랑)
                "#9775FA", // 6. 성장/자기계발 (보라)
                "#F783AC"  // 7. 관계/공감 (핑크)
            )
            Color.parseColor(palette[pos % palette.size])
        }
        adapter = valueScoreAdapter
    }

    private fun titleForEmotion(emotion: String?): String = when (emotion) {
        "행복" -> "최근 행복했던 이유는?"
        "슬픔" -> "최근 슬펐던 이유는?"
        "분노" -> "최근 화가 났던 이유는?"
        "불안" -> "최근 불안했던 이유는?"
        "평온" -> "최근 평온했던 이유는?"
        null, "" -> "최근 기분이 달라진 이유는?"
        else -> "최근 ${emotion}을(를) 느낀 이유는?"
    }

    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}