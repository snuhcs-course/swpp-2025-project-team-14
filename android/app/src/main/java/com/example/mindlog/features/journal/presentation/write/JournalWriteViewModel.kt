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

    // UI 상태를 관리하는 StateFlow
    val emotionScores = MutableStateFlow<Map<String, Int>>(emptyMap())
    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val gratitude = MutableStateFlow("") // 1. 감사한 일 StateFlow 추가

    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult = _saveResult.asSharedFlow()

    fun saveJournal() {
        // 현재 StateFlow에 저장된 값들을 가져온다.
        val currentTitle = title.value
        val currentContent = content.value
        val currentEmotions = emotionScores.value
        val currentGratitude = gratitude.value // 2. 감사한 일 값 가져오기

        if (currentTitle.isBlank() || currentContent.isBlank()) {
            viewModelScope.launch {
                _saveResult.emit(Result.Error(message = "제목과 내용을 모두 입력해주세요."))
            }
            return
        }

        viewModelScope.launch {
            try {
                // 3. UseCase 호출 시 gratitude 인자 추가
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
