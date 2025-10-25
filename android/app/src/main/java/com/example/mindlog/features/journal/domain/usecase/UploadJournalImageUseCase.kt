package com.example.mindlog.features.journal.domain.usecase

import android.net.Uri
import com.example.mindlog.features.journal.domain.repository.JournalRepository
import javax.inject.Inject

class UploadJournalImageUseCase @Inject constructor(
    private val repository: JournalRepository
) {
    suspend operator fun invoke(journalId: Int, imageUri: Uri) {
        repository.uploadJournalImage(journalId, imageUri)
    }
}
    