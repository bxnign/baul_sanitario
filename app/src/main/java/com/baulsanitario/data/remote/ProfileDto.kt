package com.baulsanitario.data.remote

import com.baulsanitario.domain.model.Profile
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    @SerialName("created_at") val createdAt: String
)

/** Solo el nombre: el id y created_at los genera la base de datos. */
@Serializable
data class ProfileInsertDto(
    val name: String
)

fun ProfileDto.toDomain() = Profile(
    id = id,
    name = name,
    createdAt = Instant.parse(createdAt)
)
