package com.example.mindlog.features.statistics.presentation

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.home.presentation.HomeActivity
import com.example.mindlog.features.journal.presentation.write.JournalWriteActivity
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.Emotion
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.example.mindlog.features.statistics.domain.model.JournalKeyword
import com.example.mindlog.features.statistics.domain.model.toKo
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.jolenechong.wordcloud.WordCloud
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import com.google.android.material.datepicker.MaterialDatePicker
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics), HomeActivity.FabClickListener {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent>
    private val viewModel: StatisticsViewModel by viewModels()

    private var wordCloud: WordCloud? = null
    // private var suppressChipCallback = false
    private var suppressEmotionSelectionCallback = false
    private val emotionOptions = listOf(
        Emotion.HAPPY,
        Emotion.SAD,
        Emotion.ANXIOUS,
        Emotion.CALM,
        Emotion.ANNOYED,
        Emotion.SATISFIED,
        Emotion.BORED,
        Emotion.INTERESTED,
        Emotion.LETHARGIC,
        Emotion.ENERGETIC
    )

    private var hasLoadedOnce = false
    private var wasLoading = false

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
    private fun formatRange(start: LocalDate, end: LocalDate): String =
        "${start.format(dateFormatter)} ~ ${end.format(dateFormatter)}"

    override fun onFabClick() {
        val intent = Intent(requireContext(), JournalWriteActivity::class.java)
        activityResultLauncher.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // 작성 완료 시 홈의 Journal 탭으로 이동
                (activity as? HomeActivity)?.let { homeActivity ->
                    homeActivity.navigateToJournalTab()
                }
            }
        }

        setupEmotionRatesPieChart()
        setupEmotionSpinner()
        setupEmotionTrendChart()


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
                .setTitleText("기간으로 검색")
                .setTheme(R.style.ThemeOverlay_MindLog_DatePicker)
                .setPositiveButtonText("검색")
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
                    syncEmotionSelectionFromState(s.selectedEmotion)
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
                        renderEmotionTrend(s.emotionTrends, s.startDate, s.endDate)
                        renderEmotionEvents(s.emotionEvents, s.selectedEmotion)
                        renderWordCloud(s.journalKeywords)
                    }
                }
            }
        }

        // 최초 로드
        viewModel.load()
    }

    private fun setupEmotionSpinner() {
        val labels = listOf("모든 감정") + emotionOptions.map { toKo(it) ?: it.name }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            labels
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        binding.spinnerEmotions.adapter = adapter

        binding.spinnerEmotions.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (suppressEmotionSelectionCallback) return

                if (position == 0) {
                    // "모든 감정" 선택 → 필터 해제
                    viewModel.clearEmotionFilter()
                } else {
                    val emotion = emotionOptions.getOrNull(position - 1) ?: return
                    viewModel.setEmotion(emotion)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // no-op
            }
        }
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

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // 주요 감정 조각: 감정별 고정 색상 사용
        rest.forEach { rate ->
            val label = toKo(rate.emotion) ?: rate.emotion.name
            entries.add(PieEntry(rate.percentage * 100f, label))
            colors.add(emotionColorFor(rate.emotion))
        }

        // 기타 조각: 중립적인 회색 계열
        if (etc > 0f) {
            entries.add(PieEntry(etc * 100f, "기타"))
            colors.add("#B0BEC5".toColorInt()) // 연한 블루그레이
        }

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

        // 키워드 리스트 구성 (가중치만 반영)
        val words = ArrayList<String>()
        keywords.forEach { k ->
            val weight = k.count.coerceAtLeast(1)
            repeat(weight * 2) {
                words.add(k.keyword)
            }
        }

        try {
            wordCloud?.apply {
                setWords(words, topN = 30)
            }
        } catch (_: Exception) { }
    }

    // ------------------------------
    // 감정 변화 (LineChart)
    // ------------------------------
    private fun setupEmotionTrendChart() = binding.chartEmotionTrend.apply {
        description.isEnabled = false
        setTouchEnabled(false)
        setScaleEnabled(false)
        isDoubleTapToZoomEnabled = false
        setPinchZoom(false)

        axisRight.isEnabled = false
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            setDrawGridLines(false)
            textColor = Color.DKGRAY
            setDrawLimitLinesBehindData(true)
        }
        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 4.5f

        legend.apply {
            isEnabled = true
            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
            orientation = Legend.LegendOrientation.HORIZONTAL
            textColor = "#636779".toColorInt()
            textSize = 10f
            isWordWrapEnabled = true
            xEntrySpace = 6f
            yEntrySpace = 4f
            xOffset = 0f
            maxSizePercent = 0.95f
        }
    }

    private fun renderEmotionTrend(
        data: List<EmotionTrend>,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
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

        val sets = data.map { trend ->
            val entries = trend.trend.mapIndexed { i, v -> Entry(i.toFloat(), v.toFloat()) }
            val lineColor = emotionColorFor(trend.emotion)
            LineDataSet(entries, toKo(trend.emotion) ?: trend.emotion.name).apply {
                color = lineColor
                lineWidth = 2f
                setDrawCircles(false)      // 데이터 포인트 점 숨김
                circleRadius = 0f          // 점 크기 0으로
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
        }

        // X축의 마지막 인덱스를 기준으로 날짜 라벨 + "오늘" 기준선 표시
        val maxIndex = data.maxOfOrNull { it.trend.lastIndex }
        val chart = binding.chartEmotionTrend
        val xAxis = chart.xAxis
        xAxis.removeAllLimitLines()

        if (maxIndex != null && maxIndex >= 0) {
            val totalDays = java.time.temporal.ChronoUnit.DAYS
                .between(startDate, endDate)
                .toInt()
                .coerceAtLeast(0)

            val labelFormatter = object : ValueFormatter() {
                private val df = DateTimeFormatter.ofPattern("M.d")

                override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                    val index = value.toInt()
                    if (index < 0 || index > maxIndex) return ""

                    // 데이터 포인트 수(N)와 기간 일수(D)를 비례 매핑
                    if (maxIndex == 0 || totalDays == 0) {
                        return endDate.format(df)
                    }
                    val dayOffset = ((totalDays.toFloat() * index) / maxIndex)
                        .toInt()
                        .coerceAtLeast(0)

                    val date = startDate.plusDays(dayOffset.toLong())
                    return date.format(df)   // 예: 9.1, 9.7 ...
                }
            }

            xAxis.valueFormatter = labelFormatter
            xAxis.granularity = 1f                    // 0,1,2,... 단위로만 라벨
            xAxis.setLabelCount((maxIndex + 1).coerceAtMost(6), false)

            // "오늘" 기준선 (마지막 인덱스 위치)
            val todayLine = LimitLine(maxIndex.toFloat(), "오늘").apply {
                lineWidth = 1.2f
                lineColor = "#2D3142".toColorInt()
                textColor = "#2D3142".toColorInt()
                textSize = 10f
                enableDashedLine(8f, 4f, 0f)
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            }
            xAxis.addLimitLine(todayLine)

            // X축 범위도 인덱스에 맞게 설정
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = maxIndex.toFloat()
        }

        chart.data = LineData(sets)
        chart.invalidate()
    }

    private fun syncEmotionSelectionFromState(selected: Emotion?) {
        val index = if (selected == null) {
            0
        } else {
            val i = emotionOptions.indexOf(selected)
            if (i == -1) return
            i + 1   // 감정들은 1부터 시작
        }

        if (binding.spinnerEmotions.selectedItemPosition == index) return
        suppressEmotionSelectionCallback = true
        binding.spinnerEmotions.setSelection(index)
        suppressEmotionSelectionCallback = false
    }

    // 감정별 고정 색상 (그래프 라인 색)
    private fun emotionColorFor(emotion: Emotion): Int = when (emotion) {
        Emotion.HAPPY      -> "#FFB300".toColorInt()  // 따뜻하고 밝은 앰버 (행복)
        Emotion.SAD        -> "#1E88E5".toColorInt()  // 깊은 블루 (슬픔)
        Emotion.ANXIOUS    -> "#C2185B".toColorInt()  // 긴장감 있는 마젠타 (불안)
        Emotion.CALM       -> "#26A69A".toColorInt()  // 차분한 티얼 블루그린 (평온)
        Emotion.ANNOYED    -> "#E53935".toColorInt()  // 강한 레드 (짜증/분노)
        Emotion.SATISFIED  -> "#7CB342".toColorInt()  // 편안한 그린 (만족)
        Emotion.BORED      -> "#757575".toColorInt()  // 뉴트럴 그레이 (지루함)
        Emotion.INTERESTED -> "#8E24AA".toColorInt()  // 호기심 있는 퍼플 (흥미)
        Emotion.LETHARGIC  -> "#6D4C41".toColorInt()  // 무거운 브라운 (무기력)
        Emotion.ENERGETIC  -> "#FF6F00".toColorInt()  // 강렬한 오렌지 (활력)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        wordCloud = null
    }
}