package com.baulsanitario.domain.usecase

import android.net.Uri
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.repository.DocumentRepository

class DownloadDocumentUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(document: Document): Result<Uri> =
        repository.downloadDocument(document)
}
