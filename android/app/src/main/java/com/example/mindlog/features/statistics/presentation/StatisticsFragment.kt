package com.example.mindlog.features.statistics.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.mindlog.R
import com.example.mindlog.databinding.FragmentSelfAwareBinding
import com.example.mindlog.databinding.FragmentStatisticsBinding
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
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
                        renderEmotionRatio(s.emotionRatios)
                        renderEmotionTrend(s.emotionTrends)
                    }
                }
            }
        }
    }
    // ------------------------------
    // 감정 비율 그래프 (HorizontalBarChart)
    // ------------------------------
    private fun setupEmotionRatioChart() = binding.chartEmotionRatio.apply {
        description.isEnabled = false
        legend.isEnabled = false
        setDrawGridBackground(false)
        axisLeft.isEnabled = false
        axisRight.isEnabled = false
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.DKGRAY
            setDrawGridLines(false)
        }
    }

    private fun renderEmotionRatio(data: List<EmotionRatio>) {
        if (data.isEmpty()) return

        val entries = data.mapIndexed { index, e ->
            BarEntry(index.toFloat(), e.value)
        }

        val dataSet = BarDataSet(entries, "감정 비율").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextColor = Color.DKGRAY
            valueTextSize = 12f
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.6f
        binding.chartEmotionRatio.data = barData

        binding.chartEmotionRatio.xAxis.valueFormatter =
            IndexAxisValueFormatter(data.map { it.emotion })
        binding.chartEmotionRatio.invalidate()
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