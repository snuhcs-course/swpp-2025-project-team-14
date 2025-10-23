package com.example.mindlog.features.selfaware.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Time
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.TodayQA
import com.example.mindlog.features.selfaware.domain.usecase.GetHistoryUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SelfAwareViewModel @Inject constructor(
    private val getTodayQAUseCase: GetTodayQAUseCase,
    private val submitAnswerUsecase: SubmitAnswerUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val date: LocalDate = Time.todayKST(),
        val questionId: Int? = null,
        val questionText: String? = null,
        val categoriesKo: List<String>? = null,
        val isAnsweredToday: Boolean = false,
        val answerText: String = "",
        val valueLabels: List<String> = listOf("성장", "관계", "안정", "자유", "성취", "재미", "윤리"),
        val valueScores: List<Float> = listOf(80f, 30f, 50f, 80f, 90f, 60f, 90f),
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    fun load() = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true, error = null) }

        val todayQARes = getTodayQAUseCase(_state.value.date)

        _state.update { s ->
            val t: TodayQA? = (todayQARes as? Result.Success)?.data
            s.copy(
                isLoading = false,
                questionId = t?.question?.id,
                questionText = t?.question?.text,
                categoriesKo = t?.question?.categoriesKo,
                isAnsweredToday = t?.answer != null,
                error = listOf(todayQARes).filterIsInstance<Result.Error>().firstOrNull()?.message
            )
        }
    }

    fun updateAnswerText(text: String) = _state.update { it.copy(answerText = text) }

    fun submit() = viewModelScope.launch(dispatcher.io) {
        val st = _state.value
        val qid = st.questionId ?: run {
            _state.update { it.copy(error = "질문을 불러오지 못했습니다.") }; return@launch
        }
        if (st.answerText.isBlank()) {
            _state.update { it.copy(error = "답변을 입력해주세요.") }; return@launch
        }
        _state.update { it.copy(isSubmitting = true, error = null) }
        when (val res = submitAnswerUsecase(qid, st.answerText)) {
            is Result.Success -> {
                // 오늘 완료 + 최신 점수 반영
                _state.update { it.copy(isSubmitting = false, isAnsweredToday = true) }
            }
            is Result.Error -> _state.update { it.copy(isSubmitting = false, error = res.message) }
        }
    }
}