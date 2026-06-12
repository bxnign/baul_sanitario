package com.baulsanitario.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baulsanitario.BaulSanitarioApp
import com.baulsanitario.ui.viewmodels.DocumentDetailState
import com.baulsanitario.ui.viewmodels.DocumentDetailViewModel

@Composable
fun DocumentDetailScreen(filePath: String) {
    val context = LocalContext.current
    val app = context.applicationContext as BaulSanitarioApp
    val viewModel: DocumentDetailViewModel = viewModel(key = filePath) {
        DocumentDetailViewModel(app.container.storageDataSource, filePath)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()
    var openError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        if (state is DocumentDetailState.Ready) {
            val signedUrl = (state as DocumentDetailState.Ready).signedUrl
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(signedUrl))
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                openError = "No hay una aplicación instalada para abrir PDFs"
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val error = openError
        if (error != null) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            when (val currentState = state) {
                is DocumentDetailState.Loading -> CircularProgressIndicator()

                is DocumentDetailState.Ready -> Text(
                    text = "Abriendo documento...",
                    style = MaterialTheme.typography.bodyMedium
                )

                is DocumentDetailState.Error -> {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { /* el ViewModel no expone retry, se resuelve navegando hacia atrás */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}
