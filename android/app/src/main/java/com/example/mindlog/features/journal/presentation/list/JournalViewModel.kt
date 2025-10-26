package com.example.mindlog.features.journal.presentation.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat // 1. SimpleDateFormat import 추가
import java.util.Locale // 2. Locale import 추가
import javax.inject.Inject

// private val JournalViewModel.serverDateFormat: Any // 3. 이 줄을 완전히 삭제합니다.

@HiltViewModel
class JournalViewModel @Inject constructor(
    private val getJournalsUseCase: GetJournalUseCase
) : ViewModel() {

    private val _journals = MutableLiveData<List<JournalEntry>>(emptyList())
    val journals: MutableLiveData<List<JournalEntry>> = _journals

    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    // --- 페이지네이션 상태 관리를 위한 변수 추가 ---
    private var nextCursor: Int? = null
    private var isLastPage = false

    // 4. SimpleDateFormat 인스턴스를 클래스 멤버로 선언하고 초기화합니다.
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    companion object {
        private const val PAGE_SIZE = 20
    }

    // loadJournals는 이제 첫 페이지 또는 새로고침 시 호출
    fun loadJournals() {
        // 상태 초기화
        nextCursor = null
        isLastPage = false
        _journals.value = emptyList() // 목록 비우기
        loadMoreJournals() // 첫 페이지 로드 시작
    }

    // 스크롤이 끝에 도달했을 때 호출될 함수
    fun loadMoreJournals() {
        // 이미 로딩 중이거나 마지막 페이지라면 중복 호출 방지
        if (isLoading.value == true || isLastPage) {
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                // UseCase에 현재 cursor 값을 전달하여 다음 페이지 요청
                val response = getJournalsUseCase(limit = PAGE_SIZE, cursor = nextCursor)

                // DTO를 UI 모델로 변환
                val newEntries = response.items.map { item ->
                    JournalEntry(
                        id = item.id,
                        title = item.title,
                        content = item.content,
                        createdAt = try {
                            // 이제 serverDateFormat은 SimpleDateFormat 타입이므로 .parse() 호출이 가능합니다.
                            serverDateFormat.parse(item.createdAt)!!
                        } catch (e: Exception) {
                            java.util.Date() // 파싱 실패 시 현재 시간으로 대체 (안전장치)
                        }
                    )
                }


                // 기존 목록에 새로 불러온 목록을 추가
                val currentList = _journals.value ?: emptyList()
                _journals.value = currentList + newEntries

                // 다음 페이지를 위한 cursor 값 업데이트
                nextCursor = response.nextCursor

                // nextCursor가 null이면 마지막 페이지임
                if (nextCursor == null) {
                    isLastPage = true
                }

            } catch (e: Exception) {
                errorMessage.value = "데이터를 불러오는 중 오류가 발생했습니다: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }
}
