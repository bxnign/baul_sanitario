package com.baulsanitario.domain.usecase

import com.baulsanitario.domain.model.Profile
import com.baulsanitario.domain.repository.ProfileRepository

class GetProfilesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): Result<List<Profile>> = repository.getProfiles()
}
