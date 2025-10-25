package com.example.mindlog.features.journal.data.repository

import ImageUploadCompleteRequest
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.mindlog.features.journal.data.api.JournalApi
import com.example.mindlog.features.journal.data.dto.*
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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

    override suspend fun uploadJournalImage(journalId: Int, imageUri: Uri) {
        val (fileName, contentType) = getFileInfoFromUri(imageUri)
            ?: throw IllegalArgumentException("유효하지 않은 파일 Uri입니다.")

        val presignedUrlResponse = journalApi.generatePresignedUrl(
            journalId = journalId,
            request = ImageUploadRequest(filename = fileName, contentType = contentType)
        )

        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
            val fileBytes = inputStream.readBytes()

            val uploadResponse = journalApi.uploadImageToS3(
                url = presignedUrlResponse.presignedUrl,
                fileBody = fileBytes,
                contentType = contentType
            )

            if (!uploadResponse.isSuccessful) {
                throw RuntimeException("S3에 이미지 업로드를 실패했습니다.")
            }
        }

        journalApi.completeImageUpload(
            journalId = journalId,
            request = ImageUploadCompleteRequest(s3Key = presignedUrlResponse.s3Key)
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
}
