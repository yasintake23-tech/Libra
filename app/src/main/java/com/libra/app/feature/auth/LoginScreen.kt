package com.libra.app.feature.auth

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.libra.app.core.state.UiState
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.components.AppButton
import com.libra.app.ui.components.AppButtonVariant

@Composable
fun LoginScreen(
    authState: UiState<UserProfile?>,
    onGoogleSignInClick: () -> Unit,
    onEmailSignIn: (email: String, password: String) -> Unit,
    onEmailRegister: (email: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = authState is UiState.Loading

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var registerMode by rememberSaveable { mutableStateOf(false) }

    val errorMessage = (authState as? UiState.Error)?.error?.message

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BoxedLogo()

            Spacer(Modifier.height(16.dp))

            Text(
                "Libra",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Her hikâyenin bir yeri var.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(28.dp))

        AppButton(
            text = "Google ile devam et",
            onClick = onGoogleSignInClick,
            isLoading = isLoading,
            variant = AppButtonVariant.PRIMARY,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(Modifier.weight(1f))
            Text(
                "  veya  ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("E-posta") },
            placeholder = { Text("ornek@mail.com") },
            singleLine = true,
            colors = fieldColors
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Şifre") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            colors = fieldColors
        )

        if (registerMode) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Yeni hesap için şifren en az 6 karakter olmalı.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        errorMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))

        AppButton(
            text = if (registerMode) "Hesap oluştur" else "E-posta ile devam et",
            onClick = {
                if (registerMode) {
                    onEmailRegister(email, password)
                } else {
                    onEmailSignIn(email, password)
                }
            },
            isLoading = isLoading,
            enabled = email.isNotBlank() && password.isNotBlank(),
            variant = AppButtonVariant.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(4.dp))

        AppButton(
            text = if (registerMode) "Zaten hesabım var" else "Yeni hesap oluştur",
            onClick = { registerMode = !registerMode },
            variant = AppButtonVariant.TEXT,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "Kitaplarını keşfet, kendi hikâyelerini yaz ve okur topluluğuna katıl.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxedLogo() {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Default.AutoStories,
            contentDescription = "Libra",
            tint = MaterialTheme.colorScheme.background,
            modifier = Modifier.size(38.dp)
        )
    }
}