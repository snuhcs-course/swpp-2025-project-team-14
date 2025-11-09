package com.example.mindlog.features.journal.presentation.write

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindlog.core.common.Result
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import com.example.mindlog.features.journal.domain.usecase.CreateJournalUseCase
import com.example.mindlog.features.journal.domain.usecase.GenerateImageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.ByteArrayOutputStream

@HiltViewModel
class JournalWriteViewModel @Inject constructor(
    private val createJournalUseCase: CreateJournalUseCase,
    private val journalRepository: JournalRepository,
    private val generateImageUseCase: GenerateImageUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

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

    val emotionScores = MutableStateFlow(initialEmotionScores)
    val title = MutableStateFlow("")
    val content = MutableStateFlow("")
    val gratitude = MutableStateFlow("")

    val selectedImageUri = MutableStateFlow<Uri?>(null)

    val generatedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val isLoading = MutableStateFlow(false)
    val aiGenerationError = MutableSharedFlow<String>()
    val noImage = MutableSharedFlow<Boolean>()
    private val _saveResult = MutableSharedFlow<Result<Unit>>()
    val saveResult = _saveResult.asSharedFlow()

    fun setGalleryImageUri(uri: Uri?) {
        if (uri != null) {
            selectedImageUri.value = uri
            generatedImageBitmap.value = null // AI 이미지 초기화
        }
    }

    fun generateImage(style: String) {
        val textContent = content.value.ifBlank { title.value }
        if (textContent.isBlank()) {
            viewModelScope.launch {
                aiGenerationError.emit("이미지를 생성하려면 내용이나 제목을 먼저 입력해주세요.")
            }
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            try {
                // 1. UseCase를 통해 Base64 이미지 문자열 요청
                val base64Image = generateImageUseCase(style, textContent)

                // 2. Base64 문자열을 Bitmap으로 디코딩
                val imageBytes = Base64.decode(base64Image, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                // 3. 생성된 Bitmap을 StateFlow에 할당하여 UI에 전달
                generatedImageBitmap.value = decodedBitmap

                // 4. 갤러리에서 선택한 이미지가 있었다면 초기화하여 AI 이미지만 선택된 상태로 만듦
                selectedImageUri.value = null

            } catch (e: Exception) {
                aiGenerationError.emit(e.message ?: "이미지 생성에 실패했습니다.")
            } finally {
                isLoading.value = false
            }
        }
    }

    fun saveJournal() {
        val currentTitle = title.value
        val currentContent = content.value
        val currentEmotions = emotionScores.value
        val currentGratitude = gratitude.value

        val galleryImageUri = selectedImageUri.value
        val aiGeneratedBitmap = generatedImageBitmap.value

        if (currentTitle.isBlank() || currentContent.isBlank() || currentGratitude.isBlank()) {
            viewModelScope.launch {
                _saveResult.emit(Result.Error(message = "제목, 내용, 감사한 일을 모두 입력해주세요."))
            }
            return
        }

        viewModelScope.launch {
            var journalId: Int? = null // journalId를 try 블록 밖에서 선언
            try {
                val emotionsToSend = currentEmotions.filter { it.value > 0 }
                // 1. 텍스트 일기 먼저 생성
                val journalResponse = createJournalUseCase(
                    title = currentTitle,
                    content = currentContent,
                    emotions = emotionsToSend,
                    gratitude = currentGratitude
                )
                journalId = journalResponse.id // 생성된 일기 ID 저장

                val imageData: Triple<ByteArray, String, String>? = when {
                    galleryImageUri != null -> {
                        context.contentResolver.openInputStream(galleryImageUri)?.use { inputStream ->
                            val bytes = inputStream.readBytes()
                            val type = context.contentResolver.getType(galleryImageUri) ?: "image/jpeg"
                            val name = "gallery_image_${System.currentTimeMillis()}.jpg"
                            Triple(bytes, type, name)
                        }
                    }
                    aiGeneratedBitmap != null -> {
                        val stream = ByteArrayOutputStream()
                        aiGeneratedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                        val bytes = stream.toByteArray()
                        val type = "image/jpeg"
                        val name = "ai_image_${System.currentTimeMillis()}.jpg"
                        Triple(bytes, type, name)
                    }
                    else -> null
                }

                // 2. 업로드할 이미지 데이터가 있다면 업로드 실행
                if (imageData != null) {
                    val (bytes, contentType, fileName) = imageData
                    journalRepository.uploadJournalImage(
                        journalId = journalId,
                        imageBytes = bytes,
                        contentType = contentType,
                        fileName = fileName
                    )
                }

                // 3. 성공 알림
                _saveResult.emit(Result.Success(Unit))

            } catch (e: Exception) {
                Log.e("JournalSaveError", "저장 실패", e)
                _saveResult.emit(Result.Error(message = e.message ?: "알 수 없는 오류가 발생했습니다."))
                return@launch // 저장 실패 시 키워드 분석을 시도하지 않고 종료
            }

            // ✨ [핵심 수정] 일기 저장 성공 후, 키워드 분석 요청
            journalId?.let { id ->
                try {
                    Log.d("JournalWriteViewModel", "일기 작성 완료. 키워드 분석을 시작합니다. (ID: $id)")
                    // UseCase를 직접 사용하기 위해 Repository를 통해 호출
                    journalRepository.extractKeywords(id)
                    Log.d("JournalWriteViewModel", "키워드 분석 요청 성공. (ID: $id)")
                } catch (e: Exception) {
                    // 키워드 분석이 실패하더라도 이미 일기 저장은 성공했으므로, 에러만 로그로 남기고 무시.
                    Log.e("JournalWriteViewModel", "키워드 분석 요청 실패 (ID: $id)", e)
                }
            }
        }
    }

    fun clearSelectedImage() {
        selectedImageUri.value = null
        generatedImageBitmap.value = null
        viewModelScope.launch {
            noImage.emit(true)
        }
    }

    fun updateEmotionScore(emotionName: String, score: Int) {
        val currentScores = emotionScores.value.toMutableMap()
        currentScores[emotionName] = score
        emotionScores.value = currentScores
    }
}
