package com.example.mindlog.features.statistics.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.chip.ChipGroup
import com.jolenechong.wordcloud.WordCloud
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    private var wordCloud: WordCloud? = null
    private var suppressChipCallback = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        setupEmotionTrendChart()
        setupEmotionRatesPieChart()

        // Chip -> 선택 감정 변경
        val chipGroup = binding.root.findViewById<ChipGroup>(R.id.chipGroupEmotions)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (suppressChipCallback) return@setOnCheckedStateChangeListener
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val selectedEmotion = when (checkedId) {
                R.id.chipHappy    -> "행복"
                R.id.chipSad      -> "슬픔"
                R.id.chipAnxious  -> "불안"
                R.id.chipLethargy -> "무기력"
                R.id.chipExciting -> "흥미"
                else -> null
            }
            selectedEmotion?.let(viewModel::setEmotion)
        }

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    // Chip UI 동기화 (상태 -> UI)
                    syncChipSelectionFromState(s.selectedEmotion)

                    if (!s.isLoading) {
                        renderEmotionRates(s.emotionRatios)
                        renderEmotionTrend(s.emotionTrends)
                        renderEmotionEvents(s.emotionEvents, s.selectedEmotion)
                        renderWordCloud(s.journalKeywords)
                    }
                }
            }
        }

        // 최초 로드
        viewModel.load()
    }

    // ------------------------------
    // 감정 비율 (PieChart)
    // ------------------------------
    private fun setupEmotionRatesPieChart() = binding.chartEmotionRates.apply {
        description.isEnabled = false
        setUsePercentValues(true)
        setDrawEntryLabels(false) // 조각 안 텍스트 끔. 값은 바깥쪽에만

        holeRadius = 55f
        transparentCircleRadius = 60f
        setHoleColor(Color.TRANSPARENT)

        setMinAngleForSlices(8f) // 너무 얇은 조각 보정

        legend.apply {
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            orientation = Legend.LegendOrientation.HORIZONTAL
            textColor = Color.parseColor("#636779")
            textSize = 12f
            isWordWrapEnabled = true
        }

        setExtraOffsets(8f, 8f, 8f, 8f)
        animateY(700)
        invalidate()
    }

    private fun renderEmotionRates(raw: List<EmotionRate>) {
        if (raw.isEmpty()) {
            binding.chartEmotionRates.clear()
            binding.chartEmotionRates.invalidate()
            return
        }

        // 3% 미만 합쳐서 "기타"
        val threshold = 0.03f
        val (small, rest) = raw.partition { it.percentage < threshold }
        val etc = small.sumOf { it.percentage.toDouble() }.toFloat()
        val merged = buildList {
            addAll(rest)
            if (etc > 0f) add(EmotionRate(emotion = "기타", count = 0, percentage = etc))
        }

        val entries = merged.map { PieEntry(it.percentage * 100f, it.emotion) }

        val palette = listOf(
            Color.parseColor("#D36B6B"),
            Color.parseColor("#FFD700"),
            Color.parseColor("#00BFA5"),
            Color.parseColor("#F06292"),
            Color.parseColor("#9575CD"),
            Color.parseColor("#FFB74D"),
            Color.parseColor("#81C784"),
            Color.parseColor("#7986CB"),
            Color.parseColor("#FF7043"),
            Color.parseColor("#4DD0E1"),
            Color.parseColor("#AED581"),
            Color.parseColor("#BA68C8"),
            Color.parseColor("#FBC02D")
        )
        val colors = List(entries.size) { i -> palette[i % palette.size] }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = Color.parseColor("#2D3142")
            valueTextSize = 12f
            sliceSpace = 2.5f

            // 값/라벨 바깥으로
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.5f
            valueLineWidth = 1.2f
            isValueLineVariableLength = true
            valueLineColor = Color.parseColor("#636779")
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pe: PieEntry?): String {
                    // setUsePercentValues(true) → value가 0~100
                    if (value < 3f) return "" // 3% 미만 숨김
                    val label = pe?.label ?: ""
                    return "$label ${String.format("%.0f%%", value)}"
                }
            })
        }

        binding.chartEmotionRates.data = pieData
        binding.chartEmotionRates.invalidate()
    }

    // ------------------------------
    // 감정 이벤트
    // ------------------------------
    private fun renderEmotionEvents(events: List<String>, selectedEmotion: String?) {
        val container = binding.cardEmotionEventsContainer
        container.removeAllViews()

        val context = container.context
        val title = com.google.android.material.textview.MaterialTextView(context).apply {
            val emo = selectedEmotion ?: "감정"
            text = "최근 ${emo}했던 이유는?"
            setTextColor(Color.parseColor("#636779"))
            textSize = 13f
            setPadding(0, 0, 0, 10)
        }
        container.addView(title)

        events.forEachIndexed { idx, event ->
            val tv = com.google.android.material.textview.MaterialTextView(context).apply {
                text = "${idx + 1}. $event"
                setTextColor(Color.parseColor("#2D3142"))
                textSize = 14f
                setPadding(0, 0, 0, 7)
            }
            container.addView(tv)
        }
    }

    // ------------------------------
    // 워드클라우드
    // ------------------------------
    private fun renderWordCloud(keywords: List<JournalKeyword>) {
        val frame = binding.wordCloudView
        if (wordCloud == null) {
            wordCloud = WordCloud(requireActivity().application, null)
            frame.addView(wordCloud)
        }
        val words = ArrayList(keywords.map { it.keyword })
        wordCloud?.setWords(words, topN = 10)
    }

    // ------------------------------
    // 감정 변화 (LineChart)
    // ------------------------------
    private fun setupEmotionTrendChart() = binding.chartEmotionTrend.apply {
        description.isEnabled = false
        legend.isEnabled = true
        setTouchEnabled(true)
        setPinchZoom(true)
        axisRight.isEnabled = false
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textColor = Color.DKGRAY
        }
    }

    private fun renderEmotionTrend(data: List<EmotionTrend>) {
        if (data.isEmpty()) {
            binding.chartEmotionTrend.clear()
            binding.chartEmotionTrend.invalidate()
            return
        }

        val colors = listOf(
            Color.parseColor("#6074F9"),
            Color.parseColor("#FF8C42"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#E57373"),
            Color.parseColor("#BA68C8")
        )

        val sets = data.mapIndexed { idx, trend ->
            val entries = trend.trend.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
            LineDataSet(entries, trend.emotion).apply {
                color = colors[idx % colors.size]
                lineWidth = 2f
                setCircleColor(color)
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
        }

        binding.chartEmotionTrend.data = LineData(sets)
        binding.chartEmotionTrend.invalidate()
    }

    // ------------------------------
    // Chip 동기화 (State → UI)
    // ------------------------------
    private fun syncChipSelectionFromState(selected: String?) {
        val id = when (selected) {
            "행복" -> R.id.chipHappy
            "슬픔" -> R.id.chipSad
            "불안" -> R.id.chipAnxious
            "무기력" -> R.id.chipLethargy
            "흥미" -> R.id.chipExciting
            else -> null
        } ?: return

        val group = binding.chipGroupEmotions
        if (group.checkedChipId == id) return
        suppressChipCallback = true
        group.check(id)
        suppressChipCallback = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        wordCloud = null
    }
}