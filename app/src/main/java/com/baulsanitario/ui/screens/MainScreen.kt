package com.baulsanitario.ui.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baulsanitario.BaulSanitarioApp
import com.baulsanitario.domain.model.Document
import com.baulsanitario.domain.model.DocumentType
import com.baulsanitario.domain.model.Profile
import com.baulsanitario.ui.components.AppTextField
import com.baulsanitario.ui.components.BottomNavBar
import com.baulsanitario.ui.components.CategoryChip
import com.baulsanitario.ui.components.DocumentCard
import com.baulsanitario.ui.components.MainTab
import com.baulsanitario.ui.components.PrimaryButton
import com.baulsanitario.ui.components.ProfileAvatar
import com.baulsanitario.ui.viewmodels.DocumentListState
import com.baulsanitario.ui.viewmodels.DocumentListViewModel
import com.baulsanitario.ui.viewmodels.ProfileSessionState
import com.baulsanitario.ui.viewmodels.ProfileSessionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.DayOfWeek

// ---------------------------------------------------------------------------
// Estado de filtro de la lista de documentos
// ---------------------------------------------------------------------------
private data class FilterState(
    val types: Set<DocumentType> = emptySet(),
    val from: LocalDate? = null,
    val to: LocalDate? = null
) {
    val isActive: Boolean get() = types.isNotEmpty() || from != null || to != null

    fun toggleType(type: DocumentType): FilterState =
        copy(types = if (type in types) types - type else types + type)
}

private fun FilterState.matches(document: Document): Boolean {
    val date = document.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
    if (types.isNotEmpty() && document.type !in types) return false
    if (from != null && date.isBefore(from)) return false
    if (to != null && date.isAfter(to)) return false
    return true
}

@Composable
fun MainScreen(
    onScanRequested: (profileId: String) -> Unit,
    onDocumentSelected: (filePath: String) -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as BaulSanitarioApp
    val activity = context as ComponentActivity

    val sessionViewModel: ProfileSessionViewModel = viewModel(viewModelStoreOwner = activity) {
        ProfileSessionViewModel(app.container.getProfilesUseCase, app.container.createProfileUseCase)
    }
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()

    when (val session = sessionState) {
        is ProfileSessionState.Loading -> CenteredLoading()

        is ProfileSessionState.Error -> CenteredError(
            message = session.message,
            onRetry = { sessionViewModel.loadProfiles() }
        )

        is ProfileSessionState.Ready -> MainScaffold(
            app = app,
            profiles = session.profiles,
            activeProfile = session.activeProfile,
            onProfileSelected = { sessionViewModel.selectProfile(it) },
            onAddProfile = { sessionViewModel.addProfile(it) },
            onScanRequested = onScanRequested,
            onDocumentSelected = onDocumentSelected
        )
    }
}

@Composable
private fun MainScaffold(
    app: BaulSanitarioApp,
    profiles: List<Profile>,
    activeProfile: Profile,
    onProfileSelected: (String) -> Unit,
    onAddProfile: (String) -> Unit,
    onScanRequested: (String) -> Unit,
    onDocumentSelected: (String) -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.DOCUMENTS) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(FilterState()) }

    val docListViewModel: DocumentListViewModel = viewModel(key = activeProfile.id) {
        DocumentListViewModel(app.container.getDocumentsByProfileUseCase, activeProfile.id)
    }
    val docState by docListViewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                currentTab = currentTab,
                activeProfileName = activeProfile.name,
                onTabSelected = { currentTab = it },
                onScanClick = { onScanRequested(activeProfile.id) },
                onProfileClick = { showProfileSheet = true }
            )
        },
        floatingActionButton = {
            if (currentTab == MainTab.DOCUMENTS) {
                FloatingActionButton(
                    onClick = { onScanRequested(activeProfile.id) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo documento")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.DOCUMENTS -> DocumentsContent(
                    docState = docState,
                    filter = filter,
                    onToggleType = { filter = filter.toggleType(it) },
                    onSelectAll = { filter = filter.copy(types = emptySet()) },
                    onOpenFilter = { showFilterSheet = true },
                    onRetry = { docListViewModel.loadDocuments() },
                    onDocumentSelected = onDocumentSelected
                )
                MainTab.SEARCH -> SearchContent(
                    docState = docState,
                    onDocumentSelected = onDocumentSelected
                )
            }
        }
    }

    if (showProfileSheet) {
        ProfileSwitcherSheet(
            profiles = profiles,
            activeProfile = activeProfile,
            onSelect = {
                onProfileSelected(it)
                showProfileSheet = false
            },
            onAddProfileClick = {
                showProfileSheet = false
                showAddDialog = true
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    if (showAddDialog) {
        AddProfileDialog(
            onConfirm = { name ->
                onAddProfile(name)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    if (showFilterSheet) {
        FilterSheet(
            initial = filter,
            onApply = {
                filter = it
                showFilterSheet = false
            },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun DocumentsContent(
    docState: DocumentListState,
    filter: FilterState,
    onToggleType: (DocumentType) -> Unit,
    onSelectAll: () -> Unit,
    onOpenFilter: () -> Unit,
    onRetry: () -> Unit,
    onDocumentSelected: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Encabezado con el título y el botón de filtro (embudo)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Documentos",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onOpenFilter) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = "Filtrar",
                    tint = if (filter.isActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filtros rápidos por tipo (multi-selección)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CategoryChip(
                text = "Todos",
                selected = filter.types.isEmpty(),
                onClick = onSelectAll
            )
            DocumentType.entries.forEach { type ->
                CategoryChip(
                    text = type.displayName,
                    selected = type in filter.types,
                    onClick = { onToggleType(type) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val allDocuments: List<Document>? = when (docState) {
            is DocumentListState.Content -> docState.documents
            is DocumentListState.Empty -> emptyList()
            else -> null
        }

        when {
            allDocuments == null && docState is DocumentListState.Error ->
                CenteredError(message = docState.message, onRetry = onRetry)
            allDocuments == null -> CenteredLoading()
            allDocuments.isEmpty() -> CenteredMessage("No hay documentos aún")
            else -> {
                val filtered = allDocuments.filter { filter.matches(it) }
                if (filtered.isEmpty()) {
                    CenteredMessage("Sin documentos para este filtro")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { document ->
                            DocumentCard(
                                fileName = document.fileName,
                                typeName = document.type.displayName,
                                date = formatDate(document),
                                onClick = { onDocumentSelected(document.filePath) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchContent(
    docState: DocumentListState,
    onDocumentSelected: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val allDocuments: List<Document> = when (docState) {
        is DocumentListState.Content -> docState.documents
        else -> emptyList()
    }
    val results = if (query.isBlank()) allDocuments
    else allDocuments.filter { it.fileName.contains(query, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar por nombre") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (results.isEmpty()) {
            CenteredMessage(if (query.isBlank()) "Escribe para buscar" else "Sin resultados")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(results) { document ->
                    DocumentCard(
                        fileName = document.fileName,
                        typeName = document.type.displayName,
                        date = formatDate(document),
                        onClick = { onDocumentSelected(document.filePath) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    initial: FilterState,
    onApply: (FilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var draftTypes by remember { mutableStateOf(initial.types) }
    var draftFrom by remember { mutableStateOf(initial.from) }
    var draftTo by remember { mutableStateOf(initial.to) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("Tipo de documento")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DocumentType.entries.forEach { type ->
                    CategoryChip(
                        text = type.displayName,
                        selected = type in draftTypes,
                        onClick = {
                            draftTypes = if (type in draftTypes) draftTypes - type else draftTypes + type
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Fecha de subida")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val today = LocalDate.now()
                CategoryChip(
                    text = "Hoy",
                    selected = draftFrom == today && draftTo == today,
                    onClick = { draftFrom = today; draftTo = today }
                )
                CategoryChip(
                    text = "Esta semana",
                    selected = draftFrom == today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) && draftTo == today,
                    onClick = {
                        draftFrom = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        draftTo = today
                    }
                )
                CategoryChip(
                    text = "Este mes",
                    selected = draftFrom == today.withDayOfMonth(1) && draftTo == today,
                    onClick = {
                        draftFrom = today.withDayOfMonth(1)
                        draftTo = today
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePickerField(
                    label = "Desde",
                    date = draftFrom,
                    onPick = { draftFrom = it },
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Hasta",
                    date = draftTo,
                    onPick = { draftTo = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onApply(FilterState()) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Limpiar",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PrimaryButton(
                    text = "Aplicar",
                    onClick = { onApply(FilterState(draftTypes, draftFrom, draftTo)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    date: LocalDate?,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showPicker = true }
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = date?.format(dateFormatter) ?: "Cualquiera",
                style = MaterialTheme.typography.bodyLarge,
                color = if (date != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onPick(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    showPicker = false
                }) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSwitcherSheet(
    profiles: List<Profile>,
    activeProfile: Profile,
    onSelect: (String) -> Unit,
    onAddProfileClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Cambiar perfil",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            profiles.forEach { profile ->
                val isActive = profile.id == activeProfile.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(profile.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileAvatar(
                        name = profile.name,
                        size = 40.dp,
                        selected = isActive
                    )
                    Text(
                        text = if (isActive) "${profile.name} (Actual)" else profile.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(start = 14.dp)
                            .fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddProfileClick)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Añadir perfil",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 14.dp)
                        .fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AddProfileDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Nuevo perfil",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            AppTextField(
                value = name,
                onValueChange = { name = it },
                label = "Nombre",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(
                    text = "Crear",
                    color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CenteredLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CenteredError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(text = "Reintentar", onClick = onRetry)
    }
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.systemDefault())

private fun formatDate(document: Document): String = dateFormatter.format(document.createdAt)
