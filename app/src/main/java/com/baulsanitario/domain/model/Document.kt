package com.baulsanitario.domain.model

import java.time.Instant

data class Document(
    val id: String,
    val profileId: String,
    val type: DocumentType,
    val filePath: String,
    val fileName: String,
    val createdAt: Instant
)
