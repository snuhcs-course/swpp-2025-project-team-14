package com.example.mindlog.features.selfaware.presentation.viewmodel

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Time
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.CategoryScore
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetValueMapUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetTopValueScoresUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetPersonalityInsightUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import java.time.LocalDate
import javax.inject.Inject


@HiltViewModel
class SelfAwareViewModel @Inject constructor(
    private val getTodayQAUseCase: GetTodayQAUseCase,
    private val submitAnswerUseCase: SubmitAnswerUseCase,
    private val getValueMapUseCase: GetValueMapUseCase,
    private val getTopValueScoresUseCase: GetTopValueScoresUseCase,
    private val getPersonalityInsightUseCase: GetPersonalityInsightUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val date: LocalDate = Time.todayKST(),
        val isAnsweredToday: Boolean = false,
        val questionId: Int? = null,
        val questionText: String? = null,
        val answerText: String = "",

        val valueCategories: List<String> = listOf("성장", "관계", "안정", "자유", "성취", "재미", "윤리"),
        val categoryScores: List<CategoryScore> = emptyList(),
        val topValueScores: List<ValueScore> = emptyList(),
        val personalityInsight: String = "",
        val comment: String = "",

        // 로딩/에러
        val isLoadingQuestion: Boolean = true,
        val isSubmitting: Boolean = false,
        val isLoading: Boolean = false,

        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pollJob: Job? = null

    fun load() = viewModelScope.launch(dispatcher.io) {
        // 화면 전체 스피너는 해제, 질문 섹션만 로딩 상태 유지
        val date = _state.value.date
        _state.update { it.copy(isLoadingQuestion = true, isLoading = true, error = null) }

        var errorMessage: String? = null
        pollJob?.cancel()

        try {
            coroutineScope {
                val valueMap = async { getValueMapUseCase(Unit) }
                val topValues = async { getTopValueScoresUseCase(Unit) }
                val insight = async { getPersonalityInsightUseCase(Unit) }
                val todayQA = async { getTodayQAUseCase(date) }

                // 결과 병합
                val valueMapRes = valueMap.await()
                val topRes = topValues.await()
                val insightRes = insight.await()
                val todayQARes = todayQA.await()

                val updated = _state.value.copy(
                    categoryScores = (valueMapRes as? Result.Success)?.data?.categoryScores ?: emptyList(),
                    topValueScores = (topRes as? Result.Success)?.data?.valueScores ?: emptyList(),
                    personalityInsight = (insightRes as? Result.Success)?.data?.personalityInsight.orEmpty(),
                    comment = (insightRes as? Result.Success)?.data?.comment.orEmpty(),
                    isLoading = false,
                    error = listOfNotNull(
                        (valueMapRes as? Result.Error)?.message,
                        (topRes as? Result.Error)?.message,
                        (insightRes as? Result.Error)?.message
                    ).firstOrNull()
                )

                _state.value = updated

                when (todayQARes) {
                    is Result.Success -> {
                        val qaRes = todayQARes.data
                        if (qaRes?.question != null) {
                            _state.update {
                                it.copy(
                                    questionId = qaRes.question.id,
                                    questionText = qaRes.question.text,
                                    isAnsweredToday = qaRes.answer != null,
                                    isLoadingQuestion = false
                                )
                            }
                        } else startPolling()
                    }

                    is Result.Error -> {
                        val code = todayQARes.code
                        if (code == null) {
                            startPolling()
                        } else {
                            _state.update {
                                it.copy(
                                    questionId = null,
                                    questionText = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                    isLoadingQuestion = false,
                                    isAnsweredToday = false,
                                    error = todayQARes.message
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            _state.update { it.copy(isLoadingQuestion = false, error = e.message ?: "self aware data loading error") }
        }
    }

    /** polling 로직 분리 */
    private fun startPolling(
        maxWaitMs: Long = 60_000L,
        initialDelayMs: Long = 1_500L,
        intervalMs: Long = 2_000L,
        maxIntervalMs: Long = 6_000L
    ) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch(dispatcher.io) {
            // 폴링 시작: 질문 섹션만 로딩 유지
            _state.update { it.copy(isLoadingQuestion = true, error = null) }

            try {
                withTimeout(maxWaitMs) {
                    delay(initialDelayMs)
                    var next = intervalMs

                    while (isActive) {
                        val date = _state.value.date
                        when (val res = getTodayQAUseCase(date)) {
                            is Result.Success -> {
                                val qa = res.data
                                if (qa?.question != null) {
                                    _state.update {
                                        it.copy(
                                            questionId = qa.question.id,
                                            questionText = qa.question.text,
                                            isAnsweredToday = qa.answer != null,
                                            isLoadingQuestion = false,
                                            error = null
                                        )
                                    }
                                    return@withTimeout
                                }
                            }
                            is Result.Error -> {
                                val code = res.code
                                if (code != null) {
                                    _state.update {
                                        it.copy(
                                            questionId = null,
                                            questionText = "질문 생성에 문제가 있습니다. 잠시 후 다시 시도해주세요.",
                                            isLoadingQuestion = false,
                                            isAnsweredToday = false,
                                            error = res.message
                                        )
                                    }
                                    return@withTimeout
                                }
                            }
                        }

                        delay(next)
                        next = (next * 1.5f).toLong().coerceAtMost(maxIntervalMs) // 지수 백오프
                    }
                }
            } catch (_: TimeoutCancellationException) {
                _state.update {
                    it.copy(
                        isLoadingQuestion = false,
                        error = "질문 생성이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    fun updateAnswerText(text: String) = _state.update { it.copy(answerText = text) }

    fun submit() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value
        if (s.questionId == null) {
            _state.update { it.copy(error = "질문을 불러오는 중 입니다.") }
            return@launch
        }
        if (s.answerText.isBlank()) {
            _state.update { it.copy(error = "답변을 입력해주세요.") }
            return@launch
        }

        _state.update { it.copy(isSubmitting = true) }

        when (val res = submitAnswerUseCase(s.questionId, s.answerText)) {
            is Result.Success -> {
                pollJob?.cancel()
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        isAnsweredToday = true,
                        isLoadingQuestion = false,
                        answerText = ""
                    )
                }
            }
            is Result.Error -> {
                _state.update { it.copy(isSubmitting = false, error = res.message) }
            }
        }
    }
}