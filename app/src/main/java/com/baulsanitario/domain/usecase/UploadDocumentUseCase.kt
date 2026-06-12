package com.baulsanitario.domain.usecase

import android.net.Uri
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType
import com.baulsanitario.domain.repository.DocumentRepository

class UploadDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(
        profileId: String,
        type: DocumentType,
        pdfUri: Uri,
        fileName: String
    ): Result<Document> = repository.uploadDocument(profileId, type, pdfUri, fileName)
}
