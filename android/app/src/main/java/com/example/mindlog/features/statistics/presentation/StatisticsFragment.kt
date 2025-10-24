package com.example.mindlog.features.statistics.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


@AndroidEntryPoint
class StatisticsFragment : Fragment(R.layout.fragment_statistics) {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: StatisticsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentStatisticsBinding.bind(view)

        viewModel.load()

        setupEmotionRatioChart()
        setupEmotionTrendChart()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { s ->
                    if (!s.isLoading) {
                        // renderEmotionRatio(s.emotionRatios)
                        renderEmotionTrend(s.emotionTrends)
                    }
                }
            }
        }
    }
    // ------------------------------
    // 감정 비율 그래프 (HorizontalBarChart)
    // ------------------------------

    private fun setupEmotionRatioChart() = binding.rvEmotionRatio.apply {
        layoutManager = LinearLayoutManager(requireContext())
        adapter = EmotionRatioAdapter().also { ad ->
            ad.submitList(
                listOf(
                    EmotionRatioPair(
                        EmotionRatio("슬픔", 30f),
                        EmotionRatio("행복", 70f)
                    ),
                    EmotionRatioPair(
                        EmotionRatio("불안", 45f),
                        EmotionRatio("자신감", 55f)
                    ),
                    EmotionRatioPair(
                        EmotionRatio("짜증/분노", 40f),
                        EmotionRatio("편안", 60f)
                    ),
                    EmotionRatioPair(
                        EmotionRatio("지루함", 65f),
                        EmotionRatio("흥미/설렘", 35f)
                    ),
                    EmotionRatioPair(
                        EmotionRatio("무기력", 25f),
                        EmotionRatio("활력", 75f)
                    ),
                )
            )
        }
        isNestedScrollingEnabled = false
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