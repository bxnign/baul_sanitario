package com.baulsanitario.domain.model

import java.time.Instant

data class Profile(
    val id: String,
    val name: String,
    val createdAt: Instant
)
