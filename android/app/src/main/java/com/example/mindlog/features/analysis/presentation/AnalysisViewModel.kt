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
        val happyReasons = listOf(
            "가족과 함께 웃으며 저녁 시간을 보냈다",
            "팀 프로젝트를 성공적으로 마무리했다",
            "산책하면서 가을 공기를 느꼈다",
            "좋아하는 음악을 들으며 하루를 정리했다",
            "친구와 오랜만에 만나 즐겁게 대화했다"
        )

        val anxiousReasons = listOf(
            "내일 있을 발표 준비가 부족하다고 느꼈다",
            "일정이 밀려서 마음이 조급했다",
            "다른 사람의 평가를 지나치게 의식했다",
            "미래에 대한 걱정이 머릿속을 떠나지 않았다",
            "잠들기 전에 여러 생각이 떠올라 뒤척였다"
        )

        val sadReasons = listOf(
            "소중한 사람과의 대화에서 상처를 받았다",
            "계획했던 일이 잘 풀리지 않았다",
            "혼자 있는 시간이 길어져 외로움을 느꼈다",
            "지나간 일들을 자꾸 떠올리며 후회했다",
            "의욕이 없어 하루 종일 무기력했다"
        )

        val angryReasons = listOf(
            "업무 중 다른 사람의 말에 기분이 상했다",
            "노력한 결과가 제대로 인정받지 못했다",
            "교통체증 때문에 약속 시간에 늦었다",
            "의견 충돌로 가족과 언성이 높아졌다",
            "작은 실수에도 스스로에게 화가 났다"
        )

        val calmReasons = listOf(
            "차분히 책을 읽으며 마음을 정리했다",
            "명상을 하며 하루를 돌아봤다",
            "정리된 방 안에서 커피 한 잔을 즐겼다",
            "조용히 음악을 들으며 마음의 여유를 느꼈다",
            "하루를 계획대로 보내며 안정감을 느꼈다"
        )

        val reasons = when (emotion) {
            "행복" -> happyReasons
            "불안" -> anxiousReasons
            "슬픔" -> sadReasons
            "분노" -> angryReasons
            "평온" -> calmReasons
            else -> listOf(
                "가족과 즐거운 시간을 보냈다",
                "프로젝트 목표를 달성했다",
                "산책/운동으로 기분을 전환했다",
                "친구와의 대화로 위로를 받았다",
                "새 취미를 시도했다"
            )
        }

        val categories = listOf("성장", "관계", "자율", "안정", "성취", "건강", "여가")
        val fixedScores = listOf(80f, 75f, 60f, 70f, 85f, 65f, 90f)
        val valueScores = categories.zip(fixedScores).map { (value, score) ->
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