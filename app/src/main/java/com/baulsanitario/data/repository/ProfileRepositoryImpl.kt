package com.baulsanitario.data.repository

import com.baulsanitario.data.remote.SupabaseDatabaseDataSource
import com.baulsanitario.data.remote.toDomain
import com.baulsanitario.domain.model.Profile
import com.baulsanitario.domain.repository.ProfileRepository

class ProfileRepositoryImpl(
    private val dataSource: SupabaseDatabaseDataSource
) : ProfileRepository {

    override suspend fun getProfiles(): Result<List<Profile>> =
        dataSource.getProfiles().map { list -> list.map { it.toDomain() } }

    override suspend fun createProfile(name: String): Result<Profile> =
        dataSource.insertProfile(name).map { it.toDomain() }
}
