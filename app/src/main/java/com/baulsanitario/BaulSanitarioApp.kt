package com.baulsanitario

import android.app.Application
import android.content.Context
import android.util.Log
import com.baulsanitario.data.local.ProfilePreferencesDataSource
import com.baulsanitario.data.remote.SupabaseAuthDataSource
import com.baulsanitario.data.remote.SupabaseClientProvider
import com.baulsanitario.data.remote.SupabaseDatabaseDataSource
import com.baulsanitario.data.remote.SupabaseStorageDataSource
import com.baulsanitario.data.repository.AuthRepositoryImpl
import com.baulsanitario.data.repository.DocumentRepositoryImpl
import com.baulsanitario.data.repository.ProfileRepositoryImpl
import com.baulsanitario.domain.repository.AuthRepository
import com.baulsanitario.domain.repository.DocumentRepository
import com.baulsanitario.domain.repository.ProfileRepository
import com.baulsanitario.domain.usecase.CreateProfileUseCase
import com.baulsanitario.domain.usecase.GetDocumentsByProfileUseCase
import com.baulsanitario.domain.usecase.GetProfilesUseCase
import com.baulsanitario.domain.usecase.LoginUseCase
import com.baulsanitario.domain.usecase.LogoutUseCase
import com.baulsanitario.domain.usecase.ScanDocumentUseCase
import com.baulsanitario.domain.usecase.UploadDocumentUseCase
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BaulSanitarioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(
            context = this,
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY
        )
        verifyConnectivity()
    }

    private fun verifyConnectivity() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val result = container.supabaseClient.postgrest["profiles"].select()
                Log.d("BaulSanitario", "Conectividad OK — respuesta: ${result.data}")
            } catch (e: Exception) {
                Log.e("BaulSanitario", "Error de conectividad: ${e.message}")
            }
        }
    }
}

class AppContainer(
    context: Context,
    supabaseUrl: String,
    supabaseAnonKey: String
) {
    val supabaseClient: SupabaseClient = SupabaseClientProvider.create(supabaseUrl, supabaseAnonKey)

    private val authDataSource = SupabaseAuthDataSource(supabaseClient)
    val authRepository: AuthRepository = AuthRepositoryImpl(authDataSource)
    val loginUseCase = LoginUseCase(authRepository)
    val logoutUseCase = LogoutUseCase(authRepository)

    val scanDocumentUseCase = ScanDocumentUseCase()

    val storageDataSource = SupabaseStorageDataSource(supabaseClient, context)
    private val databaseDataSource = SupabaseDatabaseDataSource(supabaseClient)
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl(databaseDataSource)
    private val documentRepository: DocumentRepository = DocumentRepositoryImpl(storageDataSource, databaseDataSource)

    val profilePreferencesDataSource = ProfilePreferencesDataSource(context)

    val getProfilesUseCase = GetProfilesUseCase(profileRepository)
    val createProfileUseCase = CreateProfileUseCase(profileRepository)
    val getDocumentsByProfileUseCase = GetDocumentsByProfileUseCase(documentRepository)
    val uploadDocumentUseCase = UploadDocumentUseCase(documentRepository)
}
