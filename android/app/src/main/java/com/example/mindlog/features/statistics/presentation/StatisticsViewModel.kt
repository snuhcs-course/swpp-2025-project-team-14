package com.example.mindlog.features.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.statistics.data.repository.FakeStatisticsRepository
import com.example.mindlog.features.statistics.domain.model.EmotionRatio
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: FakeStatisticsRepository,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val emotionRatios: List<EmotionRatio> = emptyList(),   // 감정 비율
        val emotionTrends: List<EmotionTrend> = emptyList(),   // 감정 변화
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true) }

        when (val result = repository.getStatistics()) {
            is Result.Success -> _state.update {
                it.copy(
                    isLoading = false,
                    emotionRatios = result.data.emotionRatios,
                    emotionTrends = result.data.emotionTrends
                )
            }

            is Result.Error -> _state.update { it.copy(isLoading = false, error = result.message) }
        }
    }
}