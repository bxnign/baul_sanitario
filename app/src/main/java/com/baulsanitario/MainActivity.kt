package com.baulsanitario

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.baulsanitario.ui.screens.DocumentDetailScreen
import com.baulsanitario.ui.screens.LoginScreen
import com.baulsanitario.ui.screens.MainScreen
import com.baulsanitario.ui.screens.ScanScreen
import com.baulsanitario.ui.theme.BaulSanitarioTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as BaulSanitarioApp

        setContent {
            BaulSanitarioTheme {
                // La sesión se carga del disco de forma asíncrona al arrancar.
                // Mientras está en Initializing mostramos un splash; cuando
                // resuelve decidimos la pantalla inicial según haya sesión o no.
                val sessionStatus by app.container.supabaseClient.auth.sessionStatus
                    .collectAsStateWithLifecycle()

                when (val status = sessionStatus) {
                    is SessionStatus.Initializing -> SplashScreen()
                    else -> {
                        val startDestination =
                            if (status is SessionStatus.Authenticated) "main" else "login"
                        AppNavHost(app = app, startDestination = startDestination)
                    }
                }
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AppNavHost(app: BaulSanitarioApp, startDestination: String) {
    val navController = rememberNavController()

    // Si la sesión expira durante el uso, redirigir al login limpiando el back stack.
    SessionExpiryRedirect(app = app, navController = navController)

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainScreen(
                onScanRequested = { profileId -> navController.navigate("scan/$profileId") },
                onDocumentSelected = { filePath ->
                    navController.navigate("documentDetail?filePath=${Uri.encode(filePath)}")
                }
            )
        }

        composable("scan/{profileId}") { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: return@composable
            ScanScreen(
                profileId = profileId,
                onUploadSuccess = {
                    // Recrea "main" para refrescar la lista; el perfil activo se
                    // conserva porque su ViewModel está scopeado a la Activity.
                    navController.navigate("main") {
                        popUpTo("main") { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = "documentDetail?filePath={filePath}",
            arguments = listOf(navArgument("filePath") { type = NavType.StringType })
        ) { backStackEntry ->
            val filePath = backStackEntry.arguments?.getString("filePath") ?: return@composable
            DocumentDetailScreen(filePath = filePath)
        }
    }
}

@Composable
private fun SessionExpiryRedirect(app: BaulSanitarioApp, navController: NavHostController) {
    LaunchedEffect(Unit) {
        app.container.supabaseClient.auth.sessionStatus.collect { status ->
            if (status is SessionStatus.NotAuthenticated) {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                if (currentRoute != null && currentRoute != "login") {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }
}
