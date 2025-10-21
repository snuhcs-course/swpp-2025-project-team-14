package com.example.mindlog.features.selfaware.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnswerHistoryViewModel @Inject constructor(
    private val getHistory: GetHistoryUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val cursor: Int = 1,
        val size: Int = 20,
        val items: List<QAItem> = emptyList(),
        val isLoading: Boolean = false,
        val isEnd: Boolean = false,
        val error: String? = null
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun refresh() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        when (val res = getHistory(cursor = 1, size = s.size)) {
            is Result.Success -> _state.update {
                it.copy(
                    isLoading = false,
                    cursor = 1,
                    items = res.data.items,
                    isEnd = res.data.items.size < s.size
                )
            }
            is Result.Error -> _state.update { it.copy(isLoading = false, error = res.message) }
        }
    }

    fun loadNext() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value
        if (s.isLoading || s.isEnd) return@launch
        _state.update { it.copy(isLoading = true) }
        when (val res = getHistory(cursor = s.cursor + 1, size = s.size)) {
            is Result.Success -> _state.update {
                val more = res.data.items
                it.copy(
                    isLoading = false,
                    cursor = s.cursor + 1,
                    items = s.items + more,
                    isEnd = more.size < s.size
                )
            }
            is Result.Error -> _state.update { it.copy(isLoading = false, error = res.message) }
        }
    }
}