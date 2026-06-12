package com.baulsanitario.domain.usecase

import android.content.Intent
import android.net.Uri
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

class ScanDocumentUseCase {

    val scannerClient: GmsDocumentScanner by lazy {
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setGalleryImportAllowed(false)
            .build()
        GmsDocumentScanning.getClient(options)
    }

    fun extractPdfUri(data: Intent?): Uri? =
        GmsDocumentScanningResult.fromActivityResultIntent(data)?.pdf?.uri
}
