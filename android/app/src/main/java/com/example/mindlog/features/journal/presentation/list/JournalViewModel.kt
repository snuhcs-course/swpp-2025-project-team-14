package com.example.mindlog.features.journal.presentation.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.BuildConfig
import com.example.mindlog.core.model.JournalEntry
import com.example.mindlog.features.journal.domain.usecase.GetJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.SearchJournalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(FlowPreview::class)
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
    var isLastPage = false

    val searchQuery = MutableStateFlow<String?>(null)
    val startDate = MutableStateFlow<String?>(null)
    val endDate = MutableStateFlow<String?>(null)

    private val serverDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    companion object {
        private const val PAGE_SIZE = 10
    }

    init {
        viewModelScope.launch {
            var previousQuery: String? = searchQuery.value
            searchQuery.debounce(500).collect { currentQuery ->
                if (currentQuery != previousQuery) {
                    loadJournals()
                    previousQuery = currentQuery
                }
            }
        }
    }

    fun loadJournals() {
        nextCursor = null
        isLastPage = false
        _journals.value = emptyList()
        loadMoreJournals()
    }

    fun loadMoreJournals() {
        if (isLoading.value == true || isLastPage) return

        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null // 에러 메시지 초기화
            try {
                val response = if (isSearching()) {
                    searchJournalsUseCase(
                        startDate = startDate.value,
                        endDate = endDate.value,
                        title = searchQuery.value,
                        limit = PAGE_SIZE,
                        cursor = nextCursor
                    )
                } else {
                    getJournalsUseCase(limit = PAGE_SIZE, cursor = nextCursor)
                }

                val newEntries = response.items.map { item ->
                    val imageUrl = try {
                        item.imageS3Keys?.let { s3Key ->
                        if (s3Key.isNotBlank()) {
                            "${BuildConfig.S3_BUCKET_URL}/$s3Key"
                        } else {
                            null
                        }
                    }
                    } catch (e: Exception) {
                        Log.e("JournalViewModel", "Image URL generation failed for item ${item.id}", e)
                        null
                    }

                    val createdAt = try {
                        serverDateFormat.parse(item.createdAt)!!
                    } catch (e: Exception) {
                        Log.e("JournalViewModel", "Date parsing failed for item ${item.id}. Defaulting to now.", e)
                        Date()
                    }

                    val keywords = item.keywords?.map { dto ->
                        com.example.mindlog.core.model.Keyword(
                            keyword = dto.keyword,
                            emotion = dto.emotion,
                            summary = dto.summary,
                            weight = dto.weight
                        )
                    } ?: emptyList()

                    JournalEntry(
                        id = item.id,
                        title = item.title,
                        content = item.content,
                        createdAt = createdAt,
                        imageUrl = imageUrl,
                        keywords = keywords
                    )
                }

                val currentList = _journals.value ?: emptyList()
                _journals.value = currentList + newEntries

                nextCursor = response.nextCursor
                if (nextCursor == null) {
                    isLastPage = true
                }

            } catch (e: Exception) {
                val errorDetails = "데이터 로딩 중 오류 발생: ${e.javaClass.simpleName} - ${e.message}"
                Log.e("JournalViewModel", errorDetails, e)
                errorMessage.value = errorDetails
            } finally {
                isLoading.value = false
            }
        }
    }

    fun isSearching(): Boolean {
        return !searchQuery.value.isNullOrBlank() || startDate.value != null
    }

    fun setDateRange(start: String?, end: String?) {
        startDate.value = start
        endDate.value = end
        loadJournals()
    }

    fun clearSearchConditions() {
        searchQuery.value = null
        startDate.value = null
        endDate.value = null
        loadJournals()
    }
}
