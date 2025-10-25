package com.example.mindlog.features.selfaware.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.usecase.GetQAHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SelfAwareHistoryViewModel @Inject constructor(
    private val getHistoryUseCase: GetQAHistoryUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val next_cursor: Int? = null,
        val limit: Int = 10,
        val items: List<QAItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun refresh() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value
        _state.update { it.copy(isLoading = true, error = null) }
        when (val res = getHistoryUseCase(limit = s.limit, cursor = s.next_cursor)) {
            is Result.Success -> _state.update {
                it.copy(
                    isLoading = false,
                    next_cursor = res.data.cursor,
                    items = res.data.items
                )
            }
            is Result.Error -> _state.update { it.copy(isLoading = false, error = res.message) }
        }
    }

    fun loadNext() = viewModelScope.launch(dispatcher.io) {
        Log.d("loadNext", "called")
        val s = _state.value
        if (s.isLoading) return@launch
        _state.update { it.copy(isLoading = true) }
        when (val res = getHistoryUseCase(limit = s.limit, cursor = s.next_cursor)) {
            is Result.Success -> _state.update {
                val more = res.data.items
                it.copy(
                    isLoading = false,
                    next_cursor = res.data.cursor,
                    items = s.items + more
                )
            }
            is Result.Error -> _state.update { it.copy(isLoading = false, error = res.message) }
        }
    }
}