package com.baulsanitario.ui.viewmodels

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.baulsanitario.domain.usecase.ScanDocumentUseCase
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class ScanState {
    object Idle : ScanState()
    object RequestingPermission : ScanState()
    object Scanning : ScanState()
    data class Success(val pdfUri: Uri) : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScanDocumentViewModel(private val useCase: ScanDocumentUseCase) : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    val scannerClient: GmsDocumentScanner get() = useCase.scannerClient

    fun onScanRequested() { _state.value = ScanState.RequestingPermission }

    fun onScanStarted() { _state.value = ScanState.Scanning }

    fun onScanSuccess(data: Intent?) {
        val uri = useCase.extractPdfUri(data)
        _state.value = if (uri != null) {
            ScanState.Success(uri)
        } else {
            ScanState.Error("No se pudo obtener el documento escaneado")
        }
    }

    fun onScanError(message: String) { _state.value = ScanState.Error(message) }

    fun onPermissionDenied() {
        _state.value = ScanState.Error("Se requiere permiso de cámara para escanear documentos")
    }

    fun reset() { _state.value = ScanState.Idle }
}
