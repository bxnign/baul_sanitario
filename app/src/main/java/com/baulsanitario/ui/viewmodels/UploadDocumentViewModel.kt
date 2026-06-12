package com.baulsanitario.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.domain.model.DocumentType
import com.baulsanitario.domain.usecase.UploadDocumentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    object Success : UploadState()
    data class Error(val message: String) : UploadState()
}

class UploadDocumentViewModel(private val uploadDocumentUseCase: UploadDocumentUseCase) : ViewModel() {

    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state.asStateFlow()

    fun upload(profileId: String, type: DocumentType, pdfUri: Uri, fileName: String) {
        viewModelScope.launch {
            _state.value = UploadState.Uploading
            uploadDocumentUseCase(profileId, type, pdfUri, fileName)
                .onSuccess { _state.value = UploadState.Success }
                .onFailure { _state.value = UploadState.Error(it.message ?: "Error al subir el documento") }
        }
    }

    fun reset() {
        _state.value = UploadState.Idle
    }
}
