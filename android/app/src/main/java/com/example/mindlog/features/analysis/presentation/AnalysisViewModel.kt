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
        val periodLabel: String = "2025.10.01–2025.10.31",
        val selectedEmotion: String? = null,
        val EmotionEvents: List<String> = emptyList(),
        val valueScores: List<ValueScoreItem> = emptyList(),
        val aiInsight: String = "",
        val aiComment: String = ""
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        // 초기 로드: 월간 + 감정 필터 없음
        regenerateDummy(period = PeriodPreset.MONTH, emotion = null)
    }

    fun setPeriod(preset: PeriodPreset) = viewModelScope.launch {
        val label = when (preset) {
            PeriodPreset.WEEK -> "2025.10.18–2025.10.24"
            PeriodPreset.MONTH -> "2025.10.01–2025.10.31"
            PeriodPreset.CUSTOM -> "사용자 지정"
        }
        _state.update { it.copy(periodLabel = label, isLoading = true) }
        regenerateDummy(period = preset, emotion = _state.value.selectedEmotion)
    }

    fun setEmotionFilter(emotion: String?) = viewModelScope.launch {
        _state.update { it.copy(selectedEmotion = emotion, isLoading = true) }
        regenerateDummy(period = null, emotion = emotion)
    }

    /**
     * 더미 데이터 생성 로직
     * - EmotionEvents: 상위 5개 이유
     * - valueScores: 7개 카테고리 0~100
     * - aiInsight/aiComment: 감정 필터에 따른 요약/팁
     */
    private fun regenerateDummy(period: PeriodPreset? = null, emotion: String? = null) {
        val baseReasons = listOf(
            "가족과 즐거운 시간을 보냈다",
            "프로젝트 목표를 달성했다",
            "산책/운동으로 기분을 전환했다",
            "친구와의 대화로 위로를 받았다",
            "새 취미를 시도했다",
            "주말에 충분히 쉬었다",
            "좋은 음식을 즐겼다"
        )

        val reasons = baseReasons.shuffled().take(5)

        val categories = listOf("성장", "관계", "자율", "안정", "성취", "건강", "여가")
        val bias = when (emotion) {
            "행복" -> 15
            "불안" -> -10
            "슬픔" -> -6
            "분노" -> -8
            "평온" -> 10
            else -> 0
        }
        val valueScores = categories.map { value ->
            val score = (50 + bias + Random.nextInt(-15, 16)).coerceIn(0, 100).toFloat()
            ValueScoreItem(value = value, score = score)
        }

        val insight = when (emotion) {
            "행복" -> "대인관계와 여가 활동이 긍정 정서를 강화하고 있어요."
            "불안" -> "일/목표 관련 불확실성이 불안을 높이고 있어요."
            "슬픔" -> "에너지 저하 시 충분한 휴식과 루틴 복원이 도움이 됩니다."
            "분노" -> "갈등 상황에서 감정-사실-요구를 분리해보세요."
            "평온" -> "규칙적인 루틴과 취미가 안정감 유지에 기여합니다."
            else -> "최근 기록 전반을 바탕으로 패턴을 요약했어요."
        }

        val comment = when (emotion) {
            "행복" -> "행복을 만든 활동을 주 3회 이상 유지해보세요."
            "불안" -> "호흡 → 기록 → 할 일 분해 순서로 불안을 낮춰보세요."
            "슬픔" -> "가벼운 산책과 햇빛 노출을 오늘 20분 시도해보세요."
            "분노" -> "타임아웃 10분, 상황/감정/요구 구분 연습!"
            "평온" -> "아침 10분 루틴을 고정해 평온도를 유지하세요."
            else -> "이번 주 한 가지 작은 실천을 정하고 기록해보세요."
        }

        _state.update {
            it.copy(
                isLoading = false,
                EmotionEvents = reasons,
                valueScores = valueScores,
                aiInsight = insight,
                aiComment = comment
            )
        }
    }
}