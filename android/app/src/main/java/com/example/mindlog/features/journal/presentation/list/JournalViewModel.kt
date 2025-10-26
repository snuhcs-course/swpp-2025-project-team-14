package com.example.mindlog.features.journal.presentation.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.SearchJournalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

@OptIn(FlowPreview::class) // debounce 사용을 위한 Opt-in
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val getJournalsUseCase: GetJournalUseCase,
    private val searchJournalsUseCase: SearchJournalsUseCase
) : ViewModel() {

    private val _journals = MutableLiveData<List<JournalEntry>>(emptyList())
    val journals: MutableLiveData<List<JournalEntry>> = _journals
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    private var nextCursor: Int? = null
    var isLastPage = false // Fragment에서 참조할 수 있도록 private 제거

    // 검색 파라미터를 관리할 StateFlow
    val searchQuery = MutableStateFlow<String?>(null)
    val startDate = MutableStateFlow<String?>(null)
    val endDate = MutableStateFlow<String?>(null)

    // 날짜 포맷 (서버 응답 파싱용)
    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    companion object {
        private const val PAGE_SIZE = 10
    }

    init {
        // 검색어가 500ms 동안 변경이 없으면 자동으로 검색 실행
        viewModelScope.launch {
            var previousQuery: String? = searchQuery.value // 초기값 설정

            searchQuery.debounce(500).collect { currentQuery ->
                // [핵심 수정] 이전 값과 현재 값이 다를 때만 loadJournals() 호출
                // 이렇게 하면 "여행" -> "" 으로 바뀔 때도 호출됨
                if (currentQuery != previousQuery) {
                    loadJournals()
                    previousQuery = currentQuery // 이전 값 업데이트
                }
            }
        }
    }

    // 새 검색을 시작하거나, 검색 조건을 초기화할 때 호출
    fun loadJournals() {
        nextCursor = null
        isLastPage = false
        _journals.value = emptyList() // 목록을 비우고 새로 시작
        loadMoreJournals()
    }

    // 다음 페이지를 로드할 때 호출
    fun loadMoreJournals() {
        if (isLoading.value == true || isLastPage) return

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                // 검색 조건 유무에 따라 다른 UseCase 호출
                val response = if (isSearching()) {
                    searchJournalsUseCase(
                        startDate = startDate.value,
                        endDate = endDate.value,
                        title = searchQuery.value?.ifBlank { null }, // 빈 문자열은 null로 처리
                        limit = PAGE_SIZE,
                        cursor = nextCursor
                    )
                } else {
                    getJournalsUseCase(limit = PAGE_SIZE, cursor = nextCursor)
                }

                val newEntries = response.items.mapNotNull { item ->
                    try {
                        JournalEntry(
                            id = item.id,
                            title = item.title,
                            content = item.content,
                            createdAt = serverDateFormat.parse(item.createdAt)!!
                        )
                    } catch (e: Exception) {
                        Log.e("JournalViewModel", "Date parsing failed for: ${item.createdAt}", e)
                        null
                    }
                }

                val currentList = _journals.value ?: emptyList()
                _journals.value = currentList + newEntries

                nextCursor = response.nextCursor
                if (nextCursor == null) {
                    isLastPage = true
                }

            } catch (e: Exception) {
                errorMessage.value = "데이터 로딩 실패: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    // 검색 모드인지 판별하는 함수
    fun isSearching(): Boolean {
        return !searchQuery.value.isNullOrBlank() || startDate.value != null
    }

    // 기간 설정/해제 함수
    fun setDateRange(start: String?, end: String?) {
        startDate.value = start
        endDate.value = end
        loadJournals() // 기간 변경 시 즉시 새로 검색
    }

    // 검색 상태를 완전히 초기화하는 함수
    fun clearSearchConditions() {
        searchQuery.value = null
        startDate.value = null
        endDate.value = null
        loadJournals()
    }
}
