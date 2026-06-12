package com.baulsanitario.domain.usecase

import com.baulsanitario.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
