package com.baulsanitario.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baulsanitario.data.local.ProfilePreferencesDataSource
import com.baulsanitario.domain.model.Profile
import com.baulsanitario.domain.usecase.CreateProfileUseCase
import com.baulsanitario.domain.usecase.GetProfilesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileSessionState {
    object Loading : ProfileSessionState()
    data class Ready(
        val profiles: List<Profile>,
        val activeProfile: Profile
    ) : ProfileSessionState()
    data class Error(val message: String) : ProfileSessionState()
}

/**
 * Mantiene la lista de perfiles y cuál está activo. Se scopea a la Activity
 * para que el perfil activo sobreviva a la recreación de las pantallas (por
 * ejemplo al volver del flujo de subida).
 */
class ProfileSessionViewModel(
    private val getProfilesUseCase: GetProfilesUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val profilePreferencesDataSource: ProfilePreferencesDataSource
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileSessionState>(ProfileSessionState.Loading)
    val state: StateFlow<ProfileSessionState> = _state.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _state.value = ProfileSessionState.Loading
            getProfilesUseCase()
                .onSuccess { profiles ->
                    _state.value = if (profiles.isEmpty()) {
                        ProfileSessionState.Error("No hay perfiles registrados")
                    } else {
                        val lastActiveId = profilePreferencesDataSource.getActiveProfileId()
                        val activeProfile = profiles.firstOrNull { it.id == lastActiveId } ?: profiles.first()
                        ProfileSessionState.Ready(profiles, activeProfile)
                    }
                }
                .onFailure {
                    _state.value = ProfileSessionState.Error(it.message ?: "Error al cargar perfiles")
                }
        }
    }

    fun selectProfile(profileId: String) {
        val current = _state.value
        if (current is ProfileSessionState.Ready) {
            val target = current.profiles.firstOrNull { it.id == profileId } ?: return
            _state.value = current.copy(activeProfile = target)
            viewModelScope.launch { profilePreferencesDataSource.setActiveProfileId(profileId) }
        }
    }

    /** Crea un perfil y, si tiene éxito, lo agrega a la lista y lo deja activo. */
    fun addProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            createProfileUseCase(trimmed).onSuccess { newProfile ->
                val current = _state.value
                _state.value = if (current is ProfileSessionState.Ready) {
                    current.copy(
                        profiles = current.profiles + newProfile,
                        activeProfile = newProfile
                    )
                } else {
                    ProfileSessionState.Ready(listOf(newProfile), newProfile)
                }
                profilePreferencesDataSource.setActiveProfileId(newProfile.id)
            }
        }
    }
}
