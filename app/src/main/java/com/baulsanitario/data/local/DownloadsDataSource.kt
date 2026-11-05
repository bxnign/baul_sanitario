package com.baulsanitario.data.local

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException

/**
 * Guarda PDFs en la carpeta pública Descargas del dispositivo mediante
 * MediaStore.Downloads, que no requiere permisos desde Android 10 (API 29).
 */
class DownloadsDataSource(private val context: Context) {

    /** Guarda el PDF en Descargas y retorna el Uri del archivo creado. */
    fun savePdf(displayName: String, bytes: ByteArray): Result<Uri> = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            throw IOException("Descargar documentos requiere Android 10 o superior")
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, toPdfFileName(displayName))
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            // Mientras IS_PENDING = 1 el archivo no es visible para otras apps.
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("No se pudo crear el archivo en Descargas")

        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: throw IOException("No se pudo escribir el archivo en Descargas")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Exception) {
            // No dejar un archivo a medias en Descargas.
            resolver.delete(uri, null, null)
            throw e
        }
        uri
    }

    /**
     * Busca en Descargas un PDF con ese nombre. En Android 10+ la app solo ve
     * los archivos que ella misma creó, así que únicamente detecta descargas
     * hechas desde Baúl Sanitario. Retorna null si no existe o no se puede consultar.
     */
    fun findExistingPdf(displayName: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return runCatching {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Downloads._ID),
                "${MediaStore.Downloads.DISPLAY_NAME} = ?",
                arrayOf(toPdfFileName(displayName)),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        cursor.getLong(0)
                    )
                } else null
            }
        }.getOrNull()
    }

    /** Quita caracteres no válidos en nombres de archivo y asegura la extensión .pdf. */
    private fun toPdfFileName(name: String): String {
        val clean = name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Documento" }
        return if (clean.endsWith(".pdf", ignoreCase = true)) clean else "$clean.pdf"
    }
}
