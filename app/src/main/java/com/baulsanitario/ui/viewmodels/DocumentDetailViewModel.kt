package com.baulsanitario.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.data.remote.SupabaseStorageDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DocumentDetailState {
    object Loading : DocumentDetailState()
    data class Ready(val signedUrl: String) : DocumentDetailState()
    data class Error(val message: String) : DocumentDetailState()
}

class DocumentDetailViewModel(
    private val storageDataSource: SupabaseStorageDataSource,
    private val filePath: String
) : ViewModel() {

    private val _state = MutableStateFlow<DocumentDetailState>(DocumentDetailState.Loading)
    val state: StateFlow<DocumentDetailState> = _state.asStateFlow()

    init {
        loadSignedUrl()
    }

    private fun loadSignedUrl() {
        viewModelScope.launch {
            storageDataSource.createSignedUrl(filePath)
                .onSuccess { _state.value = DocumentDetailState.Ready(it) }
                .onFailure { _state.value = DocumentDetailState.Error(it.message ?: "Error al obtener el documento") }
        }
    }
}
