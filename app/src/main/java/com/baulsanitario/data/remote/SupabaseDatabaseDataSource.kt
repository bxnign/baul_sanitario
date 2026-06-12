package com.baulsanitario.data.remote

import com.baulsanitario.domain.model.Document
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

class SupabaseDatabaseDataSource(private val client: SupabaseClient) {

    suspend fun getProfiles(): Result<List<ProfileDto>> = runCatching {
        client.postgrest["profiles"].select().decodeList()
    }

    suspend fun insertProfile(name: String): Result<ProfileDto> = runCatching {
        client.postgrest["profiles"].insert(ProfileInsertDto(name)) {
            select()
        }.decodeSingle()
    }

    suspend fun getDocumentsByProfile(profileId: String): Result<List<DocumentDto>> = runCatching {
        client.postgrest["documents"].select {
            filter {
                eq("profile_id", profileId)
            }
        }.decodeList()
    }

    suspend fun insertDocument(dto: DocumentDto): Result<Document> = runCatching {
        client.postgrest["documents"].insert(dto)
        dto.toDomain()
    }
}
