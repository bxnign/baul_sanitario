package com.baulsanitario.data.remote

import android.content.Context
import android.net.Uri
import com.baulsanitario.domain.model.DocumentType
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import java.io.IOException
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class SupabaseStorageDataSource(
    private val client: SupabaseClient,
    private val context: Context
) {
    suspend fun createSignedUrl(filePath: String): Result<String> = runCatching {
        client.storage.from("medical-documents").createSignedUrl(filePath, 3600.seconds)
    }

    suspend fun uploadPdf(profileId: String, type: DocumentType, pdfUri: Uri): Result<String> = runCatching {
        val bytes = context.contentResolver.openInputStream(pdfUri)?.use { it.readBytes() }
            ?: throw IOException("No se pudo leer el archivo PDF")

        val fileName = "${UUID.randomUUID()}.pdf"
        val filePath = "$profileId/${type.name}/$fileName"

        client.storage.from("medical-documents").upload(filePath, bytes)
        filePath
    }
}
