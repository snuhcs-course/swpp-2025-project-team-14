package com.example.mindlog.features.selfaware.presentation.viewmodel

import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.core.common.Time
import com.example.mindlog.core.dispatcher.DispatcherProvider
import com.example.mindlog.features.selfaware.domain.model.QAItem
import com.example.mindlog.features.selfaware.domain.model.CategoryScore
import com.example.mindlog.features.selfaware.domain.model.ValueScore
import com.example.mindlog.features.selfaware.domain.usecase.GetTodayQAUseCase
import com.example.mindlog.features.selfaware.domain.usecase.SubmitAnswerUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetValueMapUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetTopValueScoresUseCase
import com.example.mindlog.features.selfaware.domain.usecase.GetPersonalityInsightUseCase
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
    private val getValueMapUseCase: GetValueMapUseCase,
    private val getTopValueScoresUseCase: GetTopValueScoresUseCase,
    private val getPersonalityInsightUseCase: GetPersonalityInsightUseCase,
    private val submitAnswerUsecase: SubmitAnswerUseCase,
    private val dispatcher: DispatcherProvider
) : ViewModel() {

    data class UiState(
        val date: LocalDate = Time.todayKST(),
        val isAnsweredToday: Boolean = false,

        // 오늘의 질문/답변
        val questionId: Int? = null,
        val questionText: String? = null,
        val answerText: String = "",

        // 가치 맵(라벨 고정 순서 + 점수)
        val valueCatogories: List<String> = listOf("성장", "관계", "안정", "자유", "성취", "재미", "윤리"),
        val categoryScores: List<CategoryScore> = emptyList(),

        // 상위 가치 키워드 & 인사이트
        val topValueScores: List<ValueScore> = emptyList(),
        val personalityInsight: String = "",
        val comment: String = "",

        // 로딩/에러
        val isLoading: Boolean = false,
        val isSubmitting: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    private var pollJob: Job? = null

    fun load() = viewModelScope.launch(dispatcher.io) {
        _state.update { it.copy(isLoading = true, error = null) }

        val date = _state.value.date

        val todayQARes = getTodayQAUseCase(date)
        val valueMapRes = getValueMapUseCase(Unit)
        val topValuesRes = getTopValueScoresUseCase(Unit)
        val insightRes = getPersonalityInsightUseCase(Unit)

        var firstError: String? = null

        // 2) 가치 맵
        val categories: List<CategoryScore> = when (valueMapRes) {
            is Result.Success -> valueMapRes.data.categoryScores
            is Result.Error -> {
                if (firstError == null) firstError = valueMapRes.message
                emptyList()
            }
        }

        // 3) 상위 가치 점수
        val topValues: List<ValueScore> = when (topValuesRes) {
            is Result.Success -> topValuesRes.data.valueScores
            is Result.Error -> {
                if (firstError == null) firstError = topValuesRes.message
                emptyList()
            }
        }

        // 4) 인사이트
        val (commentText, insightText) = when (insightRes) {
            is Result.Success -> insightRes.data.comment to insightRes.data.personalityInsight
            is Result.Error -> {
                if (firstError == null) firstError = insightRes.message
                "" to ""
            }
        }

        // 1) 오늘의 질문/답변
        val todayQA: QAItem? = when (todayQARes) {
            is Result.Success -> todayQARes.data
            is Result.Error -> {
                firstError = todayQARes.message
                null
            }
        }

        _state.update { s ->
            s.copy(
                // 질문/답변(없으면 아직 생성 중)
                questionId = todayQA?.question?.id,
                questionText = todayQA?.question?.text,
                isAnsweredToday = todayQA?.answer != null,

                // 분석 섹션
                categoryScores = categories,
                topValueScores = topValues,
                comment = commentText,
                personalityInsight = insightText,

                // 아직 질문이 없으면 로딩 유지, 있으면 해제
                isLoading = todayQA?.question == null,
                error = firstError
            )
        }

        // 아직 오늘 질문이 생성되지 않았다면 폴링 시작
        if (todayQA?.question == null) {
            pollJob?.cancel()
            pollJob = launch(dispatcher.io) { pollTodayQuestion() }
        }
    }

    private suspend fun pollTodayQuestion(
        maxWaitMs: Long = 60_000L,
        initialDelayMs: Long = 1_500L,
        intervalMs: Long = 2_000L,
        maxIntervalMs: Long = 6_000L
    ) {
        val start = SystemClock.elapsedRealtime()
        var delayMs = intervalMs

        // 첫 안정화 대기
        delay(initialDelayMs)

        while (SystemClock.elapsedRealtime() - start < maxWaitMs) {
            val date = _state.value.date
            when (val res = getTodayQAUseCase(date)) {
                is Result.Success -> {
                    val qa = res.data
                    if (qa?.question != null) {
                        _state.update { s ->
                            s.copy(
                                isLoading = false,
                                questionId = qa.question.id,
                                questionText = qa.question.text,
                                isAnsweredToday = qa.answer != null,
                                error = null
                            )
                        }
                        return
                    } else {
                        // 아직 없음 → 계속 로딩 유지
                        _state.update { it.copy(isLoading = true, error = null) }
                    }
                }
                is Result.Error -> {
                    // 서버가 준비 중(404/204 등)인 경우도 있으니 사용자 에러는 숨기고 재시도
                    _state.update { it.copy(isLoading = true) }
                }
            }

            delay(delayMs)
            delayMs = (delayMs * 1.5f).toLong().coerceAtMost(maxIntervalMs)
        }

        // 타임아웃
        _state.update { it.copy(isLoading = false, error = "질문 생성이 지연되고 있어요. 잠시 후 다시 시도해 주세요.") }
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }

    fun updateAnswerText(text: String) = _state.update { it.copy(answerText = text) }

    fun submit() = viewModelScope.launch(dispatcher.io) {
        val s = _state.value
        val qid = s.questionId ?: run {
            _state.update { it.copy(error = "질문을 불러오지 못했습니다.") }
            return@launch
        }
        if (s.answerText.isBlank()) {
            _state.update { it.copy(error = "답변을 입력해주세요.") }
            return@launch
        }
        _state.update { it.copy(isSubmitting = true, error = null) }

        when (val res = submitAnswerUsecase(qid, s.answerText)) {
            is Result.Success -> {
                _state.update { it.copy(isSubmitting = false, isAnsweredToday = true) }
            }
            is Result.Error -> {
                _state.update { it.copy(isSubmitting = false, error = res.message) }
            }
        }
    }
}