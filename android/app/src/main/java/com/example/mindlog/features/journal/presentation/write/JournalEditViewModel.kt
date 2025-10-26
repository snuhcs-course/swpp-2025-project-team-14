package com.example.mindlog.features.journal.presentation.write

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result // 수정된 Result.kt를 사용
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.domain.usecase.DeleteJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.UpdateJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalEditViewModel @Inject constructor(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase
) : ViewModel() {

    // UI 상태: 로딩, 성공, 실패 등
    private val _journalState = MutableLiveData<Result<JournalItemResponse>>()
    val journalState: LiveData<Result<JournalItemResponse>> = _journalState

    // 수정/삭제 결과 이벤트
    private val _editResult = MutableSharedFlow<Result<String>>()
    val editResult = _editResult.asSharedFlow()

    // UI 데이터 바인딩용
    val title = MutableLiveData<String>()
    val content = MutableLiveData<String>()
    val gratitude = MutableLiveData<String>()
    private var journalId: Int? = null

    // 원본 데이터 저장용
    private var originalJournal: JournalItemResponse? = null

    fun loadJournalDetails(id: Int) {
        journalId = id
        if (_journalState.value is Result.Success) return // 이미 로딩했다면 다시 로드하지 않음

        viewModelScope.launch {
            try {
                val journal = getJournalByIdUseCase(id)
                originalJournal = journal // 원본 데이터 저장
                // 성공 시 UI 데이터 업데이트
                title.value = journal.title
                content.value = journal.content
                gratitude.value = journal.gratitude
                _journalState.value = Result.Success(journal)
            } catch (e: Exception) {
                // 👇 [수정] 현재 Result.Error 클래스에 맞게 수정
                _journalState.value = Result.Error(message = e.message ?: "일기를 불러오는데 실패했습니다.")
            }
        }
    }

    fun updateJournal() {
        val id = journalId ?: return
        val originalData = originalJournal ?: return
        val newTitle = title.value ?: ""
        val newContent = content.value ?: ""
        val newGratitude = gratitude.value ?: ""

        if (newTitle.isBlank() || newContent.isBlank() || newGratitude.isBlank()) {
            viewModelScope.launch {
                // 👇 [수정]
                _editResult.emit(Result.Error(message = "제목, 내용, 감사한 일을 모두 입력해주세요."))
            }
            return
        }

        viewModelScope.launch {
            try {
                updateJournalUseCase(
                    journalId = id,
                    originalJournal = originalData,
                    newTitle = newTitle,
                    newContent = newContent,
                    newGratitude = newGratitude
                )
                _editResult.emit(Result.Success("수정 완료"))
            } catch (e: Exception) {
                // 👇 [수정]
                _editResult.emit(Result.Error(message = e.message ?: "수정에 실패했습니다."))
            }
        }
    }

    fun deleteJournal() {
        val id = journalId ?: return

        viewModelScope.launch {
            try {
                deleteJournalUseCase(id)
                _editResult.emit(Result.Success("삭제 완료"))
            } catch (e: Exception) {
                // 👇 [수정]
                _editResult.emit(Result.Error(message = e.message ?: "삭제에 실패했습니다."))
            }
        }
    }
}
