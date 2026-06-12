package com.baulsanitario.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baulsanitario.BaulSanitarioApp
import com.baulsanitario.domain.model.DocumentType
import com.baulsanitario.ui.components.AppTextField
import com.baulsanitario.ui.components.PrimaryButton
import com.baulsanitario.ui.components.SelectablePill
import com.baulsanitario.ui.viewmodels.ScanDocumentViewModel
import com.baulsanitario.ui.viewmodels.ScanState
import com.baulsanitario.ui.viewmodels.UploadDocumentViewModel
import com.baulsanitario.ui.viewmodels.UploadState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val nameDateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

/** Nombre por defecto del documento: "Tipo_dd-MM-yyyy", con guiones bajos. */
private fun defaultDocumentName(type: DocumentType?): String {
    val base = (type?.displayName ?: "Documento").replace(" ", "_")
    return "${base}_${LocalDate.now().format(nameDateFormatter)}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(profileId: String, onUploadSuccess: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BaulSanitarioApp

    val scanViewModel: ScanDocumentViewModel = viewModel {
        ScanDocumentViewModel(app.container.scanDocumentUseCase)
    }
    val uploadViewModel: UploadDocumentViewModel = viewModel {
        UploadDocumentViewModel(app.container.uploadDocumentUseCase)
    }

    val scanState by scanViewModel.state.collectAsStateWithLifecycle()
    val uploadState by uploadViewModel.state.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf<DocumentType?>(null) }
    // null mientras el usuario no edite: el campo sigue el nombre por defecto.
    var customName by remember { mutableStateOf<String?>(null) }
    val nameValue = customName ?: defaultDocumentName(selectedType)

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) onUploadSuccess()
    }

    LaunchedEffect(scanState) {
        if (scanState is ScanState.Idle) {
            selectedType = null
            customName = null
            uploadViewModel.reset()
        }
    }

    val scannerLauncher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scanViewModel.onScanSuccess(result.data)
        } else {
            scanViewModel.onScanError("Escaneo cancelado")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            scanViewModel.onScanStarted()
            (context as Activity).let { activity ->
                scanViewModel.scannerClient
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        scanViewModel.onScanError(e.message ?: "Error al iniciar el escáner")
                    }
            }
        } else {
            scanViewModel.onPermissionDenied()
        }
    }

    fun launchScanner() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            scanViewModel.onScanStarted()
            (context as Activity).let { activity ->
                scanViewModel.scannerClient
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        scanViewModel.onScanError(e.message ?: "Error al iniciar el escáner")
                    }
            }
        } else {
            scanViewModel.onScanRequested()
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Subir documento",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            when (val currentScanState = scanState) {
                is ScanState.Idle -> IdleContent(onScan = { launchScanner() })

                is ScanState.RequestingPermission,
                is ScanState.Scanning -> LoadingContent()

                is ScanState.Success -> DocumentCapturedContent(
                    pdfUri = currentScanState.pdfUri,
                    name = nameValue,
                    onNameChange = { customName = it },
                    selectedType = selectedType,
                    uploadState = uploadState,
                    onTypeSelected = { selectedType = it },
                    onUpload = { type ->
                        val finalName = nameValue.ifBlank { defaultDocumentName(type) }
                        uploadViewModel.upload(profileId, type, currentScanState.pdfUri, finalName)
                    },
                    onRescan = { scanViewModel.reset() }
                )

                is ScanState.Error -> ScanErrorContent(
                    message = currentScanState.message,
                    onRetry = { scanViewModel.reset() }
                )
            }
        }
    }
}

@Composable
private fun IdleContent(onScan: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.DocumentScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Escanea un documento médico",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Receta, examen, boleta u orden médica",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        PrimaryButton(
            text = "Escanear documento",
            onClick = onScan,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Iniciando escáner...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScanErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(28.dp))
        PrimaryButton(
            text = "Intentar de nuevo",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DocumentCapturedContent(
    pdfUri: Uri,
    name: String,
    onNameChange: (String) -> Unit,
    selectedType: DocumentType?,
    uploadState: UploadState,
    onTypeSelected: (DocumentType) -> Unit,
    onUpload: (DocumentType) -> Unit,
    onRescan: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        // Recuadro del documento capturado, con borde celeste fino
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Documento capturado",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Listo para subir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tipo de documento",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))

        DocumentType.entries.forEach { type ->
            SelectablePill(
                text = type.displayName,
                selected = selectedType == type,
                onClick = { onTypeSelected(type) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Nombre del documento",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        AppTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Nombre",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (uploadState is UploadState.Error) {
            Text(
                text = uploadState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        PrimaryButton(
            text = "Subir documento",
            onClick = { selectedType?.let { onUpload(it) } },
            enabled = selectedType != null,
            loading = uploadState is UploadState.Uploading,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Volver a escanear",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
