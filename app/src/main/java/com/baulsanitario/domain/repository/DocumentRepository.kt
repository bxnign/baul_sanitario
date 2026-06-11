package com.baulsanitario.domain.repository

import android.net.Uri
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType

interface DocumentRepository {
    suspend fun getDocumentsByProfile(profileId: String): Result<List<Document>>
    suspend fun uploadDocument(profileId: String, type: DocumentType, pdfUri: Uri): Result<Document>
}
