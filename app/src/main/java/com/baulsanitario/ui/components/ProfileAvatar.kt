package com.baulsanitario.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Avatar circular del perfil: círculo celeste con la inicial del nombre.
 * Cuando está seleccionado muestra un borde celeste más claro alrededor.
 */
@Composable
fun ProfileAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "?"

    var box = modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary)
    if (selected) {
        box = box.border(2.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
    }
    if (onClick != null) {
        box = box.clickable(onClick = onClick)
    }

    Box(modifier = box, contentAlignment = Alignment.Center) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = (size.value * 0.42f).sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
