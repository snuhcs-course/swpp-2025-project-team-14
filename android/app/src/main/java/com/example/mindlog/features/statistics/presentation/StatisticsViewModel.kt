package com.example.mindlog.features.statistics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.statistics.data.repository.FakeStatisticsRepository
import com.example.mindlog.features.statistics.domain.model.EmotionRate
import com.example.mindlog.features.statistics.domain.model.EmotionTrend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
        val startDate: LocalDate = LocalDate.now().minusMonths(1),
        val endDate: LocalDate = LocalDate.now(),
        val emotionRatios: List<EmotionRate> = emptyList(),   // 감정 비율
        val emotionTrends: List<EmotionTrend> = emptyList(),   // 감정 변화
        val emotion: String = "행복",
        val emotionEvents: List<String> = emptyList(),
        val journalKeywords: List<String> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun load() = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true) }

        val emotion = _state.value.emotion
        val ratio = async { repository.getEmotionRatio() }
        val trend = async { repository.getEmotionTrend() }
        val events = async { repository.getEmotionEvents(emotion) }
        val keywords = async { repository.getJournalKeywords() }

        val emotionRatioResult = ratio.await()
        val emotionTrendResult = trend.await()
        val eventResult = events.await()
        val keywordsResult = keywords.await()

        _state.update { state ->
            state.copy(
                isLoading = false,
                emotionRatios = (emotionRatioResult as? Result.Success)?.data ?: emptyList(),
                emotionTrends = (emotionTrendResult as? Result.Success)?.data ?: emptyList(),
                emotionEvents = (eventResult as? Result.Success)?.data ?: emptyList(),
                journalKeywords = (keywordsResult as? Result.Success)?.data ?: emptyList(),
                error = listOfNotNull(
                    (emotionRatioResult as? Result.Error)?.message,
                    (emotionTrendResult as? Result.Error)?.message,
                    (eventResult as? Result.Error)?.message,
                    (keywordsResult as? Result.Error)?.message
                ).firstOrNull()
            )
        }
    }

    fun setEmotion(emotion: String) = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true, emotion = emotion) }

        val eventsResult = repository.getEmotionEvents(emotion)

        _state.update { s ->
            s.copy(
                isLoading = false,
                emotion = emotion,
                emotionEvents = (eventsResult as? Result.Success)?.data ?: emptyList(),
                error = (eventsResult as? Result.Error)?.message
            )
        }
    }
}