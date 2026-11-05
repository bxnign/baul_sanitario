package com.baulsanitario.domain.repository

import android.net.Uri
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType

interface DocumentRepository {
    suspend fun getDocumentsByProfile(profileId: String): Result<List<Document>>
    suspend fun uploadDocument(
        profileId: String,
        type: DocumentType,
        pdfUri: Uri,
        fileName: String
    ): Result<Document>

    /** Descarga el PDF del documento a Descargas y retorna el Uri del archivo guardado. */
    suspend fun downloadDocument(document: Document): Result<Uri>

    /** Retorna el Uri de una descarga previa de este documento, o null si no existe. */
    suspend fun findDownloadedDocument(document: Document): Uri?
}
