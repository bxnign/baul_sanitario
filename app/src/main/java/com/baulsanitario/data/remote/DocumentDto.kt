package com.baulsanitario.data.remote

import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class DocumentDto(
    val id: String,
    @SerialName("profile_id") val profileId: String,
    val type: String,
    @SerialName("file_path") val filePath: String,
    @SerialName("file_name") val fileName: String,
    @SerialName("created_at") val createdAt: String
)

fun DocumentDto.toDomain() = Document(
    id = id,
    profileId = profileId,
    type = DocumentType.valueOf(type),
    filePath = filePath,
    fileName = fileName,
    createdAt = Instant.parse(createdAt)
)
