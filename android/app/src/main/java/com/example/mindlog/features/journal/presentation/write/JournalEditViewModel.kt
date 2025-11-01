package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.data.dto.JournalItemResponse
import com.example.mindlog.features.journal.domain.usecase.DeleteJournalUseCase
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
    @ApplicationContext private val context: Context
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

    val generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)
    val aiGenerationError = MutableSharedFlow<String>()

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

    // ✨ [핵심 추가] 갤러리 이미지 선택 시 호출될 함수
    fun setGalleryImageUri(uri: Uri?) {
        if (uri != null) {
            selectedImageUri.value = uri
            generatedImageBitmap.value = null // AI 이미지 초기화
            existingImageUrl.value = null   // 기존 서버 이미지 초기화
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

                // AI 이미지 생성 성공 시, 다른 이미지 소스 초기화
                selectedImageUri.value = null
                existingImageUrl.value = null

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

        if (newTitle.isBlank() || newContent.isBlank() || newGratitude.isBlank()) {
            viewModelScope.launch {
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
    }
}
