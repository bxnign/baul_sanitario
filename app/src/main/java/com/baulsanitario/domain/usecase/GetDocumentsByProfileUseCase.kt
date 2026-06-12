package com.baulsanitario.domain.usecase

import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.repository.DocumentRepository

class GetDocumentsByProfileUseCase(private val repository: DocumentRepository) {
    suspend operator fun invoke(profileId: String): Result<List<Document>> =
        repository.getDocumentsByProfile(profileId)
}
