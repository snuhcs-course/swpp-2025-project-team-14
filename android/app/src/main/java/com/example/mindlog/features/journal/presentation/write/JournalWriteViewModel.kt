package com.example.mindlog.features.journal.presentation.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.domain.usecase.CreateJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalWriteViewModel @Inject constructor(
    private val createJournalUseCase: CreateJournalUseCase
) : ViewModel() {

    // --- 👇 [핵심] emotionScores의 초기값을 여기서 설정합니다. ---
    private val initialEmotionScores = mapOf(
        "happy" to 0,
        "sad" to 0,
        "anxious" to 0,
        "calm" to 0,
        "annoyed" to 0,
        "satisfied" to 0,
        "bored" to 0,
        "interested" to 0,
        "lethargic" to 0,
        "energetic" to 0
    )

    // 1. emotionScores의 초기값으로 위에서 정의한 initialEmotionScores를 사용합니다.
    val emotionScores = MutableStateFlow(initialEmotionScores)
    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val gratitude = MutableStateFlow("")

    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult = _saveResult.asSharedFlow()

    fun saveJournal() {
        val currentTitle = title.value
        val currentContent = content.value
        val currentEmotions = emotionScores.value
        val currentGratitude = gratitude.value

        if (currentTitle.isBlank() || currentContent.isBlank() || currentGratitude.isBlank()) {
            viewModelScope.launch {
                _saveResult.emit(Result.Error(message = "제목, 내용, 감사한 일을 모두 입력해주세요."))
            }
            return
        }

        viewModelScope.launch {
            try {
                createJournalUseCase(
                    title = currentTitle,
                    content = currentContent,
                    emotions = currentEmotions,
                    gratitude = currentGratitude
                )
                _saveResult.emit(Result.Success(Unit))
            } catch (e: Exception) {
                _saveResult.emit(Result.Error(message = e.message))
            }
        }
    }

    fun updateEmotionScore(emotionName: String, score: Int) {
        val currentScores = emotionScores.value.toMutableMap()
        currentScores[emotionName] = score
        emotionScores.value = currentScores
    }
}