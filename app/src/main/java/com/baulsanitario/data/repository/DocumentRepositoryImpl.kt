package com.baulsanitario.data.repository

import android.net.Uri
import com.baulsanitario.data.remote.DocumentDto
import com.baulsanitario.data.remote.SupabaseDatabaseDataSource
import com.baulsanitario.data.remote.SupabaseStorageDataSource
import com.baulsanitario.data.remote.toDomain
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType
import com.baulsanitario.domain.repository.DocumentRepository
import java.time.Instant
import java.util.UUID

class DocumentRepositoryImpl(
    private val storageDataSource: SupabaseStorageDataSource,
    private val databaseDataSource: SupabaseDatabaseDataSource
) : DocumentRepository {

    override suspend fun getDocumentsByProfile(profileId: String): Result<List<Document>> =
        databaseDataSource.getDocumentsByProfile(profileId).map { list -> list.map { it.toDomain() } }

    override suspend fun uploadDocument(
        profileId: String,
        type: DocumentType,
        pdfUri: Uri,
        fileName: String
    ): Result<Document> {
        // El archivo físico usa un path con UUID único; el fileName legible
        // elegido por el usuario solo se guarda como metadato para mostrar.
        val filePath = storageDataSource.uploadPdf(profileId, type, pdfUri)
            .getOrElse { return Result.failure(it) }

        val dto = DocumentDto(
            id = UUID.randomUUID().toString(),
            profileId = profileId,
            type = type.name,
            filePath = filePath,
            fileName = fileName,
            createdAt = Instant.now().toString()
        )
        return databaseDataSource.insertDocument(dto)
    }
}
