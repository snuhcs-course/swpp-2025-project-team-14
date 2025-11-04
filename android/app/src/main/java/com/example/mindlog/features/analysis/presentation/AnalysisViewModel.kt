package com.example.mindlog.features.analysis.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

enum class PeriodPreset { WEEK, MONTH, CUSTOM }

data class ValueScoreItem(
    val value: String,
    val score: Float // 0..100
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    // 나중에 API 붙일 때:
    // private val repository: AnalysisRepository,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state
}