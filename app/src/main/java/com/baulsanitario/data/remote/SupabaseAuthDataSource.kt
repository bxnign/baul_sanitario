package com.baulsanitario.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

class SupabaseAuthDataSource(private val client: SupabaseClient) {

    suspend fun login(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun logout() {
        client.auth.signOut()
    }

    fun isLoggedIn(): Boolean = client.auth.currentSessionOrNull() != null
}
