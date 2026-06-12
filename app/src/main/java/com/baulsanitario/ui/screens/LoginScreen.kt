package com.baulsanitario.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.baulsanitario.BaulSanitarioApp
import com.baulsanitario.ui.components.AppTextField
import com.baulsanitario.ui.components.PrimaryButton
import com.baulsanitario.ui.viewmodels.AuthState
import com.baulsanitario.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as BaulSanitarioApp
    val viewModel: AuthViewModel = viewModel {
        AuthViewModel(app.container.loginUseCase)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(state) {
        if (state is AuthState.Success) onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Baúl Sanitario",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tus documentos médicos, ordenados",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(44.dp))

        AppTextField(
            value = email,
            onValueChange = { email = it },
            label = "Correo electrónico",
            keyboardType = KeyboardType.Email,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (state is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (state as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = "Ingresar",
            onClick = { viewModel.login(email, password) },
            enabled = email.isNotBlank() && password.isNotBlank(),
            loading = state is AuthState.Loading,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
