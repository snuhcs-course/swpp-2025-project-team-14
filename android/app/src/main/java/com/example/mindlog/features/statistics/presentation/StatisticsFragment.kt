package com.example.mindlog.features.statistics.presentation


import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        viewModel.load()

        setupEmotionTrendChart()
        setupEmotionRatioPieChart()

        val chipGroup = binding.root.findViewById<ChipGroup>(R.id.chipGroupEmotions)
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener

            val selectedEmotion: String? = when (checkedId) {
                R.id.chipHappy    -> "행복"
                R.id.chipSad      -> "슬픔"
                R.id.chipAnxious  -> "불안"
                R.id.chipLethargy -> "무기력"
                R.id.chipExciting -> "흥미"
                else -> null
            }
            selectedEmotion?.let { viewModel.setEmotion(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    if (!s.isLoading) {
                        renderEmotionRatio(s.emotionRatios)
                        renderEmotionTrend(s.emotionTrends)
                        renderEmotionEvents(s.emotionEvents)
                        renderWordCloud(s.journalKeywords)
                    }
                }
            }
        }
    }
    // ------------------------------
    // 감정 비율 그래프 (PieChart)
    // ------------------------------
    private fun setupEmotionRatioPieChart() = binding.chartEmotionRatio.apply {
        description.isEnabled = false
        setUsePercentValues(true)

        // ⬇️ 조각 안 텍스트(Entry Label) 끄고, 밖에만 값 표시
        setDrawEntryLabels(false)

        setEntryLabelColor(Color.parseColor("#2D3142"))
        setEntryLabelTextSize(11f)
        holeRadius = 55f
        transparentCircleRadius = 60f
        setHoleColor(Color.TRANSPARENT)

        // 너무 얇은 조각이 보이도록 최소 각도 설정(필요시 조절)
        setMinAngleForSlices(8f)

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

    private fun renderEmotionRatio(raw: List<EmotionRatio>) {
        if (raw.isEmpty()) {
            binding.chartEmotionRatio.clear()
            binding.chartEmotionRatio.invalidate()
            return
        }

        // ⬇️ 3% 미만을 '기타'로 합치기 (임계값은 필요에 맞게 조절)
        val threshold = 0.03f
        val (small, rest) = raw.partition { it.ratio < threshold }
        val etcRatio = small.sumOf { it.ratio.toDouble() }.toFloat()

        val merged = mutableListOf<EmotionRatio>().apply {
            addAll(rest)
            if (etcRatio > 0f) add(EmotionRatio("기타", etcRatio))
        }

        // PieEntry 구성
        val entries = merged.map { PieEntry(it.ratio * 100f, it.emotion) } // setUsePercentValues(true)라 0~100로 주면 편함

        // 팔레트 (필요시 확장/교체)
        val baseColors = listOf(
            Color.parseColor("#D36B6B"),
            Color.parseColor("#FFD700"), // 밝은 노랑
            Color.parseColor("#00BFA5"), // 청록
            Color.parseColor("#F06292"), // 핑크
            Color.parseColor("#9575CD"), // 라벤더
            Color.parseColor("#FFB74D"), // 오렌지
            Color.parseColor("#81C784"), // 연한 초록
            Color.parseColor("#7986CB"), // 파스텔 블루
            Color.parseColor("#FF7043"), // 선셋 오렌지
            Color.parseColor("#4DD0E1"), // 시안
            Color.parseColor("#AED581"), // 연두
            Color.parseColor("#BA68C8"), // 보라
            Color.parseColor("#FBC02D")  // 골드
        )
        val colors = List(entries.size) { i -> baseColors[i % baseColors.size] }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = Color.parseColor("#2D3142")
            valueTextSize = 12f
            sliceSpace = 2.5f

            // ⬇️ 라벨을 조각 바깥으로
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            // ⬇️ 리더 라인(꺾인 선) 조정
            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.5f
            valueLineWidth = 1.2f
            isValueLineVariableLength = true
            valueLineColor = Color.parseColor("#636779")
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pe: PieEntry?): String {
                    // value는 setUsePercentValues(true)이므로 0~100 값
                    if (value < 3f) return "" // 3% 미만 라벨 숨김
                    val label = pe?.label ?: ""
                    return "$label ${String.format("%.0f%%", value)}"
                }
            })
        }

        binding.chartEmotionRatio.data = pieData
        binding.chartEmotionRatio.invalidate()
    }

    private fun renderEmotionEvents(events: List<String>) {
        val container = binding.root.findViewById<android.widget.LinearLayout>(R.id.cardEmotionEventsContainer)
        container.removeAllViews()
        // Subtitle: show current emotion
        val context = container.context
        val subtitle = com.google.android.material.textview.MaterialTextView(context).apply {
            text = "최근 ${viewModel.state.value.emotion}했던 이유는?"
            setTextColor(Color.parseColor("#636779"))
            textSize = 13f
            setPadding(0, 0, 0, 10)
        }
        container.addView(subtitle)
        events.forEachIndexed { idx, event ->
            val tv = com.google.android.material.textview.MaterialTextView(context).apply {
                text = "${idx + 1}. $event"
                setTextColor(Color.parseColor("#2D3142"))
                textSize = 14f
                setPadding(0, 0, 0, 5)
            }
            container.addView(tv)
        }
    }

    private fun renderWordCloud(keywords: List<String>) {
        val wordCloudFrame = binding.root.findViewById<FrameLayout>(R.id.wordCloudView)

        if (wordCloud == null) {
            wordCloud = WordCloud(requireActivity().application, null)
            wordCloudFrame.addView(wordCloud)
        }

        wordCloud?.setWords(ArrayList(keywords), topN = 10)
    }


    // ------------------------------
    // 감정 변화 그래프 (LineChart)
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
        if (data.isEmpty()) return

        val colors = listOf(
            Color.parseColor("#6074F9"),
            Color.parseColor("#FF8C42"),
            Color.parseColor("#4CAF50"),
            Color.parseColor("#E57373"),
            Color.parseColor("#BA68C8")
        )

        val dataSets = data.mapIndexed { index, trendData ->
            val entries = trendData.trend.mapIndexed { i, value ->
                Entry(i.toFloat(), value)
            }

            LineDataSet(entries, trendData.emotion).apply {
                color = colors[index % colors.size]
                lineWidth = 2f
                setCircleColor(color)
                circleRadius = 4f
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
        }

        val lineData = LineData(dataSets)
        binding.chartEmotionTrend.data = lineData
        binding.chartEmotionTrend.invalidate()
    }
}