package com.example.mindlog.features.journal.presentation.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.model.JournalEntry // 1. 새로 만든 core 모델 경로로 변경
// import com.example.mindlog.features.journal.domain.usecase.GetJournalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Date

@HiltViewModel
class JournalViewModel @Inject constructor(
    // private val getJournalsUseCase: GetJournalsUseCase // TODO: 의존성 주입
) : ViewModel() {

    // 2. LiveData가 JournalEntry 리스트를 담도록 타입 변경
    val journals = MutableLiveData<List<JournalEntry>>()
    val isLoading = MutableLiveData<Boolean>()
    val errorMessage = MutableLiveData<String?>()

    fun loadJournals() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                // val result = getJournalsUseCase()
                // journals.value = result

                // --- 임시 더미 데이터 ---
                // 3. 임시 데이터도 JournalEntry 타입으로 생성
                journals.value = listOf(
                    JournalEntry(1, "첫 번째 일기", "오늘 날씨는 맑음", Date()),
                    JournalEntry(2, "두 번째 일기", "코딩은 즐거워", Date()),
                    JournalEntry(3, "세 번째 일기", "내일은 주말!", Date())
                )
                // --- 임시 더미 데이터 끝 ---

            } catch (e: Exception) {
                errorMessage.value = "데이터 로딩 오류: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}
