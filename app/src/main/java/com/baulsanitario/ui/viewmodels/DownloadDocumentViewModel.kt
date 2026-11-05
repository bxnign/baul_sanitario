package com.baulsanitario.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.usecase.DownloadDocumentUseCase
import com.baulsanitario.domain.usecase.FindDownloadedDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val documentId: String) : DownloadState()
    /** El documento ya fue descargado antes: espera la decisión del usuario. */
    data class AlreadyExists(val document: Document, val existingUri: Uri) : DownloadState()
    data class Success(val fileName: String, val uri: Uri) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class DownloadDocumentViewModel(
    private val downloadDocumentUseCase: DownloadDocumentUseCase,
    private val findDownloadedDocumentUseCase: FindDownloadedDocumentUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    /** Punto de entrada desde la UI: si ya existe una descarga previa, pregunta antes. */
    fun requestDownload(document: Document) {
        if (_state.value is DownloadState.Downloading) return
        viewModelScope.launch {
            val existing = findDownloadedDocumentUseCase(document)
            if (existing != null) {
                _state.value = DownloadState.AlreadyExists(document, existing)
            } else {
                download(document)
            }
        }
    }

    /** Descarga sin verificar si ya existe (el usuario eligió descargar otra copia). */
    fun download(document: Document) {
        if (_state.value is DownloadState.Downloading) return
        viewModelScope.launch {
            _state.value = DownloadState.Downloading(document.id)
            downloadDocumentUseCase(document)
                .onSuccess { _state.value = DownloadState.Success(document.fileName, it) }
                .onFailure { _state.value = DownloadState.Error(it.message ?: "Error al descargar el documento") }
        }
    }

    /** Se llama una vez mostrado el resultado (éxito o error) al usuario. */
    fun consumeResult() {
        val current = _state.value
        if (current is DownloadState.Success || current is DownloadState.Error) {
            _state.value = DownloadState.Idle
        }
    }

    /** El usuario cerró el diálogo de "ya existe" sin elegir ninguna acción. */
    fun dismissExisting() {
        if (_state.value is DownloadState.AlreadyExists) _state.value = DownloadState.Idle
    }
}
