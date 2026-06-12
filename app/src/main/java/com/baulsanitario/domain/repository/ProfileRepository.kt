package com.baulsanitario.domain.repository

import com.baulsanitario.domain.model.Profile

interface ProfileRepository {
    suspend fun getProfiles(): Result<List<Profile>>
    suspend fun createProfile(name: String): Result<Profile>
}
