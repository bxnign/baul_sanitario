package com.baulsanitario.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// La app es dark-only por diseño: una única paleta, sin tema claro ni dynamicColor.
private val BaulColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = Outline,
    error = ErrorRed,
    onError = OnAccent
)

@Composable
fun BaulSanitarioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BaulColorScheme,
        typography = Typography,
        content = content
    )
}
