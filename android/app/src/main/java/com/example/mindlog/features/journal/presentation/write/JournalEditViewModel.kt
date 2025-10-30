package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.domain.usecase.DeleteJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.UpdateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.UploadJournalImageUseCase // ✨ [추가]
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JournalEditViewModel @Inject constructor(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase,
    private val uploadJournalImageUseCase: UploadJournalImageUseCase, // ✨ [핵심 수정] Repository 대신 UseCase 주입
    @ApplicationContext private val context: Context // ✨ [추가] Context 주입
) : ViewModel() {

    private val _journalState = MutableLiveData<Result<JournalItemResponse>>()
    val journalState: LiveData<Result<JournalItemResponse>> = _journalState
    private val _editResult = MutableSharedFlow<Result<String>>()
    val editResult = _editResult.asSharedFlow()

    val title = MutableLiveData<String>()
    val content = MutableLiveData<String>()
    val gratitude = MutableLiveData<String>()

    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val existingImageUrl = MutableStateFlow<String?>(null)

    private var journalId: Int? = null
    private var originalJournal: JournalItemResponse? = null

    fun loadJournalDetails(id: Int) {
        journalId = id
        if (_journalState.value is Result.Success) return

        viewModelScope.launch {
            try {
                val journal = getJournalByIdUseCase(id)
                originalJournal = journal
                title.value = journal.title
                content.value = journal.content
                gratitude.value = journal.gratitude
                if (!journal.imageS3Keys.isNullOrBlank()) {
                    existingImageUrl.value = "${com.example.mindlog.BuildConfig.S3_BUCKET_URL}/${journal.imageS3Keys}"
                }
                _journalState.value = Result.Success(journal)
            } catch (e: Exception) {
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
        val newImageUri = selectedImageUri.value

        if (newTitle.isBlank() || newContent.isBlank() || newGratitude.isBlank()) {
            viewModelScope.launch {
                _editResult.emit(Result.Error(message = "제목, 내용, 감사한 일을 모두 입력해주세요."))
            }
            return
        }

        viewModelScope.launch {
            try {
                // 1. 텍스트 내용 먼저 수정
                updateJournalUseCase(
                    journalId = id,
                    originalJournal = originalData,
                    newTitle = newTitle,
                    newContent = newContent,
                    newGratitude = newGratitude
                )

                // ✨ [핵심 수정] 새로 선택된 이미지가 있다면 UseCase를 통해 업로드
                if (newImageUri != null) {
                    context.contentResolver.openInputStream(newImageUri)?.use { inputStream ->
                        val bytes = inputStream.readBytes()
                        val type = context.contentResolver.getType(newImageUri) ?: "image/jpeg"
                        val name = "gallery_image_${System.currentTimeMillis()}.jpg"

                        uploadJournalImageUseCase( // Repository 대신 UseCase 호출
                            journalId = id,
                            imageBytes = bytes,
                            contentType = type,
                            fileName = name
                        )
                    }
                }

                _editResult.emit(Result.Success("수정 완료"))
            } catch (e: Exception) {
                Log.e("JournalEditError", "수정 실패", e)
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
                _editResult.emit(Result.Error(message = e.message ?: "삭제에 실패했습니다."))
            }
        }
    }

    fun clearSelectedImage() {
        selectedImageUri.value = null
        existingImageUrl.value = null
    }
}
