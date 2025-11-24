package com.example.mindlog.features.statistics.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.Emotion
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
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    private var wordCloud: WordCloud? = null
    private var suppressChipCallback = false

    private var hasLoadedOnce = false
    private var wasLoading = false

    private fun toKo(emotion: Emotion?): String? = when (emotion) {
        Emotion.HAPPY -> "행복"
        Emotion.SAD -> "슬픔"
        Emotion.ANXIOUS -> "불안"
        Emotion.CALM -> "평안"
        Emotion.ANNOYED -> "짜증"
        Emotion.SATISFIED -> "만족"
        Emotion.BORED -> "지루함"
        Emotion.INTERESTED -> "흥미"
        Emotion.LETHARGIC -> "무기력"
        Emotion.ENERGETIC -> "활력"
        null -> null
    }

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private fun formatRange(start: LocalDate, end: LocalDate): String =
        "${start.format(dateFormatter)}~${end.format(dateFormatter)}"

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
                R.id.chipHappy    -> Emotion.HAPPY
                R.id.chipSad      -> Emotion.SAD
                R.id.chipAnxious  -> Emotion.ANXIOUS
                R.id.chipLethargy -> Emotion.LETHARGIC
                R.id.chipExciting -> Emotion.INTERESTED
                else -> null
            }
            selectedEmotion?.let(viewModel::setEmotion)
        }

        // 기간 프리셋: 주간 / 월간
        binding.btnWeekly.setOnClickListener {
            val end = LocalDate.now()
            val start = end.minusDays(6) // 최근 7일
            viewModel.setDateRange(start, end)
            binding.tvPeriodRange.text = formatRange(start, end)
            viewModel.load()
        }
        binding.btnMonthly.setOnClickListener {
            val end = LocalDate.now()
            val start = end.minusDays(29) // 최근 30일
            viewModel.setDateRange(start, end)
            binding.tvPeriodRange.text = formatRange(start, end)
            viewModel.load()
        }
        // 사용자 지정: MaterialDatePicker (range)
        binding.btnCustom.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("기간 선택")
                .build()
            picker.addOnPositiveButtonClickListener { sel ->
                val startMillis = sel.first ?: return@addOnPositiveButtonClickListener
                val endMillis = sel.second ?: return@addOnPositiveButtonClickListener
                val zone = ZoneId.systemDefault()
                val start = java.time.Instant.ofEpochMilli(startMillis).atZone(zone).toLocalDate()
                val end = java.time.Instant.ofEpochMilli(endMillis).atZone(zone).toLocalDate()
                viewModel.setDateRange(start, end)
                binding.tvPeriodRange.text = formatRange(start, end)
                viewModel.load()
            }
            picker.show(parentFragmentManager, "date_range_picker")
        }

        // 상태 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    // Chip UI 동기화 (상태 -> UI)
                    syncChipSelectionFromState(s.selectedEmotion)
                    binding.tvPeriodRange.text = formatRange(s.startDate, s.endDate)

                    // 통계 로딩 상태 추적: 최초 로딩이 끝난 이후에만 empty 상태를 보여주기 위함
                    if (s.isLoading) {
                        wasLoading = true
                    }
                    if (!s.isLoading && wasLoading) {
                        hasLoadedOnce = true
                    }

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
            textColor = "#636779".toColorInt()
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

            // 최초 로딩이 끝난 이후에만 empty 상태 표시
            val showEmpty = hasLoadedOnce
            binding.chartEmotionRates.isVisible = false
            binding.emptyEmotionRates.isVisible = showEmpty
            if (showEmpty) {
                binding.lottieEmotionRatesEmpty.playAnimation()
            } else {
                binding.lottieEmotionRatesEmpty.cancelAnimation()
            }
            return
        } else {
            binding.emptyEmotionRates.isVisible = false
            binding.lottieEmotionRatesEmpty.cancelAnimation()
            binding.chartEmotionRates.isVisible = true
        }


        val threshold = 0.03f
        val (small, rest) = raw.partition { it.percentage < threshold }
        val etc = small.sumOf { it.percentage.toDouble() }.toFloat()

        val entries = buildList {
            addAll(rest.map { PieEntry(it.percentage * 100f, toKo(it.emotion) ?: it.emotion.name) })
            if (etc > 0f) add(PieEntry(etc * 100f, "기타"))
        }

        val palette = listOf(
            "#D36B6B".toColorInt(), "#FFD700".toColorInt(),
            "#00BFA5".toColorInt(), "#F06292".toColorInt(),
            "#9575CD".toColorInt(), "#FFB74D".toColorInt(),
            "#81C784".toColorInt(), "#7986CB".toColorInt(),
            "#FF7043".toColorInt(), "#4DD0E1".toColorInt(),
            "#AED581".toColorInt(), "#BA68C8".toColorInt(),
            "#FBC02D".toColorInt()
        )
        val colors = List(entries.size) { i -> palette[i % palette.size] }

        val dataSet = PieDataSet(entries, "").apply {
            setColors(colors)
            valueTextColor = "#2D3142".toColorInt()
            valueTextSize = 12f
            sliceSpace = 2.5f
            yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            valueLinePart1Length = 0.6f
            valueLinePart2Length = 0.5f
            valueLineWidth = 1.2f
            isValueLineVariableLength = true
            valueLineColor = "#636779".toColorInt()
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getPieLabel(value: Float, pe: PieEntry?): String {
                    if (value < 3f) return ""
                    val label = pe?.label ?: ""
                    return "$label ${String.format(Locale.US, "%.0f%%", value)}"
                }
            })
        }

        binding.chartEmotionRates.data = pieData
        binding.chartEmotionRates.invalidate()
    }

    // ------------------------------
    // 감정 이벤트
    // ------------------------------
    private fun toReasonPhrase(emotion: Emotion?): String = when (emotion) {
        Emotion.HAPPY      -> "행복했던"
        Emotion.SAD        -> "슬펐던"
        Emotion.ANXIOUS    -> "불안했던"
        Emotion.CALM       -> "평안했던"
        Emotion.ANNOYED    -> "짜증났던"
        Emotion.SATISFIED  -> "만족했던"
        Emotion.BORED      -> "지루했던"
        Emotion.INTERESTED -> "흥미로웠던"
        Emotion.LETHARGIC  -> "무기력했던"
        Emotion.ENERGETIC  -> "에너지가 넘쳤던"
        null               -> "그렇게 느꼈던"
    }

    private fun renderEmotionEvents(events: List<String>, selectedEmotion: Emotion?) {
        val container = binding.cardEmotionEventsContainer
        container.removeAllViews()

        val context = container.context
        val emoReason = toReasonPhrase(selectedEmotion)

        val title = com.google.android.material.textview.MaterialTextView(context).apply {
            text = "최근 ${emoReason} 이유는?"
            setTextColor("#636779".toColorInt())
            textSize = 13f
            setPadding(0, 0, 0, 10)
        }
        container.addView(title)

        if (events.isEmpty()) {
            val emptyText = com.google.android.material.textview.MaterialTextView(context).apply {
                text = "해당 기간 동안 ${emoReason} 사건이 없어요."
                setTextColor("#2D3142".toColorInt())
                textSize = 14f
                setPadding(0, 0, 0, 4)
            }
            container.addView(emptyText)
            return
        }

        events.forEachIndexed { idx, event ->
            val tv = com.google.android.material.textview.MaterialTextView(context).apply {
                text = "${idx + 1}. $event"
                setTextColor("#2D3142".toColorInt())
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
        if (keywords.isEmpty()) {
            // 최초 로딩이 끝난 이후에만 empty 상태 표시
            val showEmpty = hasLoadedOnce
            binding.wordCloudView.isVisible = false
            binding.emptyWordCloud.isVisible = showEmpty
            if (showEmpty) {
                binding.lottieWordCloudEmpty.playAnimation()
            } else {
                binding.lottieWordCloudEmpty.cancelAnimation()
            }
            return
        } else {
            binding.emptyWordCloud.isVisible = false
            binding.lottieWordCloudEmpty.cancelAnimation()
            binding.wordCloudView.isVisible = true
        }

        val frame = binding.wordCloudView
        if (wordCloud == null) {
            wordCloud = WordCloud(requireActivity().application, null)
            frame.addView(wordCloud)
        }
        val words = ArrayList(keywords.map { it.keyword })
        try {
            wordCloud?.setWords(words, topN = 10)
        } catch (_: Exception) {
            /* ignore rendering errors in tests */
        }
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

            // 최초 로딩이 끝난 이후에만 empty 상태 표시
            val showEmpty = hasLoadedOnce
            binding.chartEmotionTrend.isVisible = false
            binding.emptyEmotionTrend.isVisible = showEmpty
            if (showEmpty) {
                binding.lottieEmotionTrendEmpty.playAnimation()
            } else {
                binding.lottieEmotionTrendEmpty.cancelAnimation()
            }
            return
        } else {
            binding.emptyEmotionTrend.isVisible = false
            binding.lottieEmotionTrendEmpty.cancelAnimation()
            binding.chartEmotionTrend.isVisible = true
        }

        val colors = listOf(
            "#6074F9".toColorInt(),
            "#FF8C42".toColorInt(),
            "#4CAF50".toColorInt(),
            "#E57373".toColorInt(),
            "#BA68C8".toColorInt()
        )

        val sets = data.mapIndexed { idx, trend ->
            val entries = trend.trend.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
            LineDataSet(entries, toKo(trend.emotion) ?: trend.emotion.name).apply {
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
    private fun syncChipSelectionFromState(selected: Emotion?) {
        val id = when (selected) {
            Emotion.HAPPY -> R.id.chipHappy
            Emotion.SAD -> R.id.chipSad
            Emotion.ANXIOUS -> R.id.chipAnxious
            Emotion.LETHARGIC -> R.id.chipLethargy
            Emotion.INTERESTED -> R.id.chipExciting
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