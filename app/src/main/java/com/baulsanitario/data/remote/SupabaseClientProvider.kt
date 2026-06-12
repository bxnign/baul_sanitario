package com.baulsanitario.data.remote

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    fun create(url: String, anonKey: String) = createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = anonKey
    ) {
        install(Postgrest) {
            defaultSchema = "baul_sanitario"
        }
        install(Storage)
        install(Auth)
    }
}
