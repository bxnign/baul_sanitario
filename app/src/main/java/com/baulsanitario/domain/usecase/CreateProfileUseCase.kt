package com.baulsanitario.domain.usecase

import com.baulsanitario.domain.model.Profile
import com.baulsanitario.domain.repository.ProfileRepository

class CreateProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(name: String): Result<Profile> =
        repository.createProfile(name)
}
