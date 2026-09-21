package com.libra.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    val isLoading = authState is UiState.Loading

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AutoStories, "Libra", tint = MaterialTheme.colorScheme.background, modifier = Modifier.size(38.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text("Libra", style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(8.dp))
            Text("Her hikâyenin bir yeri var.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            AppButton(
                text = "Google ile devam et",
                onClick = onGoogleSignInClick,
                isLoading = isLoading,
                variant = AppButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Kitaplarını keşfet, kendi hikâyelerini yaz ve okur topluluğuna katıl.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
