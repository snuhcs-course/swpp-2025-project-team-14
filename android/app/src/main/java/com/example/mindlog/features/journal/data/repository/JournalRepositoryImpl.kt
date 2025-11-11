package com.example.mindlog.features.journal.data.repository

import ImageUploadCompleteRequest
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.*
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException // ✨ 올바른 HttpException을 사용합니다.
import java.net.SocketTimeoutException
import javax.inject.Inject

class JournalRepositoryImpl @Inject constructor(
    private val journalApi: JournalApi,
    @ApplicationContext private val context: Context
) : JournalRepository {

    override suspend fun createJournal(
        title: String,
        content: String,
        emotions: Map<String, Int>,
        gratitude: String
    ): JournalResponse {
        val request = JournalRequest(
            title = title,
            content = content,
            emotions = emotions,
            gratitude = gratitude
        )
        return journalApi.createJournal(request)
    }

    override suspend fun getJournals(limit: Int, cursor: Int?): JournalListResponse {
        return journalApi.getJournals(limit = limit, cursor = cursor)
    }

    override suspend fun getJournalById(journalId: Int): JournalItemResponse {
        return journalApi.getJournalById(journalId = journalId)
    }

    override suspend fun updateJournal(
        journalId: Int,
        request: UpdateJournalRequest
    ) {
        journalApi.updateJournal(journalId = journalId, request = request)
    }

    override suspend fun deleteJournal(journalId: Int) {
        journalApi.deleteJournal(journalId = journalId)
    }

    override suspend fun uploadJournalImage(
        journalId: Int,
        imageBytes: ByteArray,
        contentType: String,
        fileName: String
    ) {
        try {
            // 1. Presigned URL 생성 요청
            val presignedUrlResponse = journalApi.generatePresignedUrl(
                journalId = journalId,
                request = ImageUploadRequest(filename = fileName, contentType = contentType)
            )

            // 2. S3로 파일 업로드
            val requestBody = imageBytes.toRequestBody(
                contentType.toMediaTypeOrNull(), 0, imageBytes.size
            )

            val uploadResponse = journalApi.uploadImageToS3(
                url = presignedUrlResponse.presignedUrl,
                fileBody = requestBody,
                contentType = contentType
            )

            if (!uploadResponse.isSuccessful) {
                val errorBody = uploadResponse.errorBody()?.string()
                throw RuntimeException("S3 업로드 실패. Status: ${uploadResponse.code()}, Body: $errorBody")
            }

            // 3. 업로드 완료 보고
            journalApi.completeImageUpload(
                journalId = journalId,
                request = ImageUploadCompleteRequest(s3Key = presignedUrlResponse.s3Key)
            )
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            throw RuntimeException("API 요청 실패. Status: ${e.code()}, 에러: $errorBody", e)
        } catch (e: Exception) {
            throw RuntimeException("이미지 업로드 중 알 수 없는 오류 발생: ${e.message}", e)
        }
    }

    override suspend fun searchJournals(
        startDate: String?,
        endDate: String?,
        title: String?,
        limit: Int,
        cursor: Int?
    ): JournalListResponse {
        return journalApi.searchJournals(
            startDate = startDate,
            endDate = endDate,
            title = title,
            limit = limit,
            cursor = cursor
        )
    }

    private fun getFileInfoFromUri(uri: Uri): Pair<String, String>? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val fileName = cursor.getString(nameIndex)
                val contentType = context.contentResolver.getType(uri)
                if (fileName != null && contentType != null) {
                    fileName to contentType
                } else {
                    null
                }
            } else {
                null
            }
        }
    }

    override suspend fun generateImage(style: String, content: String): String {
        try {
            val request = GenerateImageRequest(style = style, content = content)
            val response = journalApi.generateImage(request)
            return response.imageBase64
        } catch (e: SocketTimeoutException) {
            throw RuntimeException("이미지 생성 시간이 초과되었어요. 잠시 후 다시 시도해 주세요.", e)
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun extractKeywords(journalId: Int): KeywordListResponse {
        return journalApi.extractKeywords(journalId = journalId)
    }
}

