package com.baulsanitario.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.domain.model.Profile
import com.baulsanitario.domain.usecase.GetProfilesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Content(val profiles: List<Profile>) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel(private val getProfilesUseCase: GetProfilesUseCase) : ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState.Loading)
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _state.value = HomeState.Loading
            getProfilesUseCase()
                .onSuccess { _state.value = HomeState.Content(it) }
                .onFailure { _state.value = HomeState.Error(it.message ?: "Error al cargar perfiles") }
        }
    }
}
