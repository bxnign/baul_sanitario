package com.baulsanitario.data.repository

import com.baulsanitario.data.remote.SupabaseAuthDataSource
import com.baulsanitario.domain.repository.AuthRepository

class AuthRepositoryImpl(private val dataSource: SupabaseAuthDataSource) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        dataSource.login(email, password)
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        dataSource.logout()
    }

    override fun isLoggedIn(): Boolean = dataSource.isLoggedIn()
}
