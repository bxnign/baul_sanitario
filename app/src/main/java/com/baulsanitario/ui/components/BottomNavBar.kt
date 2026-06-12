package com.baulsanitario.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Secciones de la barra inferior que mantienen estado propio. */
enum class MainTab { DOCUMENTS, SEARCH }

/**
 * Barra de navegación inferior estilo Instagram: Documentos, Buscar, Subir
 * (acción que abre el escáner) y el avatar del perfil activo (abre el selector).
 */
@Composable
fun BottomNavBar(
    currentTab: MainTab,
    activeProfileName: String,
    onTabSelected: (MainTab) -> Unit,
    onScanClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.primary,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        indicatorColor = Color.Transparent
    )

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            selected = currentTab == MainTab.DOCUMENTS,
            onClick = { onTabSelected(MainTab.DOCUMENTS) },
            icon = { Icon(Icons.Outlined.Description, contentDescription = "Documentos") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = currentTab == MainTab.SEARCH,
            onClick = { onTabSelected(MainTab.SEARCH) },
            icon = { Icon(Icons.Outlined.Search, contentDescription = "Buscar") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = false,
            onClick = onScanClick,
            icon = { Icon(Icons.Outlined.PhotoCamera, contentDescription = "Subir") },
            colors = itemColors
        )
        NavigationBarItem(
            selected = false,
            onClick = onProfileClick,
            icon = {
                ProfileAvatar(
                    name = activeProfileName,
                    size = 28.dp
                )
            },
            colors = itemColors
        )
    }
}
