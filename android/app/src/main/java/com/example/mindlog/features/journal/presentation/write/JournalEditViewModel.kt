package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.core.model.Keyword
import com.example.mindlog.features.journal.data.dto.EmotionResponse
import com.example.mindlog.features.journal.domain.usecase.DeleteJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.ExtractKeywordsUseCase
import com.example.mindlog.features.journal.domain.usecase.GenerateImageUseCase
import com.example.mindlog.features.journal.domain.usecase.GetJournalByIdUseCase
import com.example.mindlog.features.journal.domain.usecase.UpdateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.UploadJournalImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class JournalEditViewModel @Inject constructor(
    private val getJournalByIdUseCase: GetJournalByIdUseCase,
    private val updateJournalUseCase: UpdateJournalUseCase,
    private val deleteJournalUseCase: DeleteJournalUseCase,
    private val uploadJournalImageUseCase: UploadJournalImageUseCase,
    private val generateImageUseCase: GenerateImageUseCase,
    private val extractKeywordsUseCase: ExtractKeywordsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _journalState = MutableLiveData<Result<JournalItemResponse>>()
    val journalState: LiveData<Result<JournalItemResponse>> = _journalState
    private val _editResult = MutableSharedFlow<Result<String>>()
    val editResult = _editResult.asSharedFlow()

    val title = MutableLiveData<String>()
    val content = MutableLiveData<String>()
    val gratitude = MutableLiveData<String>()

    val keywords = MutableLiveData<List<Keyword>>()
    val emotions = MutableLiveData<List<EmotionResponse>>()
    val selectedImageUri = MutableStateFlow<Uri?>(null)
    val existingImageUrl = MutableStateFlow<String?>(null)

    val generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)
    val aiGenerationError = MutableSharedFlow<String>()
    val noImage = MutableSharedFlow<Boolean>()

    var journalId: Int? = null
        private set

    private var originalJournal: JournalItemResponse? = null

    fun loadJournalDetails(id: Int, forceRefresh: Boolean = false) {
        if (journalId == id && !forceRefresh) return

        journalId = id
        viewModelScope.launch {
            try {
                // 1. GET /journal/{id} 호출
                val journal = getJournalByIdUseCase(id)
                originalJournal = journal
                title.value = journal.title
                content.value = journal.content
                gratitude.value = journal.gratitude
                if (!journal.imageS3Keys.isNullOrBlank()) {
                    existingImageUrl.value = "${com.example.mindlog.BuildConfig.S3_BUCKET_URL}/${journal.imageS3Keys}"
                } else {
                    existingImageUrl.value = null
                }
                emotions.value = journal.emotions

                val uiKeywords = journal.keywords?.map { dto ->
                    Keyword(
                        keyword = dto.keyword,
                        emotion = dto.emotion,
                        summary = dto.summary,
                        weight = dto.weight
                    )
                } ?: emptyList()
                keywords.value = uiKeywords // 변환된 리스트를 할당

                _journalState.value = Result.Success(journal)

                if (journal.keywords.isNullOrEmpty()) {
                    extractJournalKeywords(id)
                }

            } catch (e: Exception) {
                _journalState.value = Result.Error(message = e.message ?: "일기를 불러오는데 실패했습니다.")
            }
        }
    }

    // 키워드 추출 로직을 담당하는 별도 함수
    private fun extractJournalKeywords(id: Int) {
        viewModelScope.launch {
            Log.d("JournalEditViewModel", "키워드가 없어 분석을 요청합니다. (ID: $id)")
            try {
                // 3. POST /analyze 요청 (DB에 키워드 저장)
                extractKeywordsUseCase(id)

                // 4. 분석 요청 성공 후, 키워드가 포함된 최신 데이터를 다시 불러오기 위해 loadJournalDetails 호출
                Log.d("JournalEditViewModel", "키워드 분석 완료. 데이터를 새로고침합니다.")
                loadJournalDetails(id, forceRefresh = true)

            } catch (e: Exception) {
                val errorMessage = "키워드 분석 실패: ${e.message}"
                Log.e("JournalEditViewModel", errorMessage, e)
                // 실패 시 특별한 처리는 하지 않음 (다음 상세페이지 진입 시 다시 시도)
            }
        }
    }


    // 갤러리 이미지 선택 시 호출될 함수
    fun setGalleryImageUri(uri: Uri?) {
        if (uri != null) {
            selectedImageUri.value = uri
            generatedImageBitmap.value = null // AI 이미지 초기화
        }
    }

    fun generateImage(style: String) {
        val textContent = content.value?.ifBlank { title.value }
        if (textContent.isNullOrBlank()) {
            viewModelScope.launch {
                aiGenerationError.emit("이미지를 생성하려면 내용이나 제목을 먼저 입력해주세요.")
            }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            try {
                val base64Image = generateImageUseCase(style, textContent)
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                generatedImageBitmap.value = decodedBitmap
                selectedImageUri.value = null

            } catch (e: Exception) {
                aiGenerationError.emit(e.message ?: "이미지 생성에 실패했습니다.")
            } finally {
                isLoading.value = false
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
        val aiGeneratedBitmap = generatedImageBitmap.value

        when {
            newTitle.isBlank() -> {
                viewModelScope.launch {
                    _editResult.emit(Result.Error(message = "제목을 입력해주세요."))
                }
                return
            }
            newContent.isBlank() -> {
                viewModelScope.launch {
                    _editResult.emit(Result.Error(message = "오늘의 하루를 입력해주세요."))
                }
                return
            }
        }

        val isTextChanged = originalData.title != newTitle ||
                originalData.content != newContent ||
                originalData.gratitude != newGratitude
        val isImageChanged = newImageUri != null || aiGeneratedBitmap != null || (originalData.imageS3Keys != null && existingImageUrl.value == null)

        if (!isTextChanged && !isImageChanged) {
            viewModelScope.launch { _editResult.emit(Result.Success("수정 완료")) }
            return
        }

        viewModelScope.launch {
            try {
                if (isTextChanged) {
                    updateJournalUseCase(
                        journalId = id,
                        originalTitle = originalData.title,
                        originalContent = originalData.content,
                        originalGratitude = originalData.gratitude,
                        newTitle = newTitle,
                        newContent = newContent,
                        newGratitude = newGratitude
                    )
                }

                val imageData: Triple<ByteArray, String, String>? = when {
                    newImageUri != null -> {
                        context.contentResolver.openInputStream(newImageUri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val type = context.contentResolver.getType(newImageUri) ?: "image/jpeg"
                            val name = "gallery_image_${System.currentTimeMillis()}.jpg"
                            Triple(bytes, type, name)
                        }
                    }
                    aiGeneratedBitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        aiGeneratedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val bytes = stream.toByteArray()
                        Triple(bytes, "image/jpeg", "ai_image_${System.currentTimeMillis()}.jpg")
                    }
                    else -> null
                }

                if (imageData != null) {
                    val (bytes, contentType, fileName) = imageData
                    uploadJournalImageUseCase(
                        journalId = id,
                        imageBytes = bytes,
                        contentType = contentType,
                        fileName = fileName
                    )
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
        generatedImageBitmap.value = null
        viewModelScope.launch {
            noImage.emit(true)
        }
    }
}
