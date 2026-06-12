package com.baulsanitario.domain.usecase

import com.baulsanitario.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> =
        repository.login(email, password)
}
