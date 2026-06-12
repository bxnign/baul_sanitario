package com.baulsanitario.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.usecase.GetDocumentsByProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DocumentListState {
    object Loading : DocumentListState()
    object Empty : DocumentListState()
    data class Content(val documents: List<Document>) : DocumentListState()
    data class Error(val message: String) : DocumentListState()
}

class DocumentListViewModel(
    private val getDocumentsByProfileUseCase: GetDocumentsByProfileUseCase,
    private val profileId: String
) : ViewModel() {

    private val _state = MutableStateFlow<DocumentListState>(DocumentListState.Loading)
    val state: StateFlow<DocumentListState> = _state.asStateFlow()

    init {
        loadDocuments()
    }

    fun loadDocuments() {
        viewModelScope.launch {
            _state.value = DocumentListState.Loading
            getDocumentsByProfileUseCase(profileId)
                .onSuccess { docs ->
                    _state.value = if (docs.isEmpty()) DocumentListState.Empty
                    else DocumentListState.Content(docs)
                }
                .onFailure { _state.value = DocumentListState.Error(it.message ?: "Error al cargar documentos") }
        }
    }
}
