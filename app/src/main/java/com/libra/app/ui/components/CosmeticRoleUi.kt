package com.libra.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.CosmeticRole
import kotlinx.coroutines.delay

@Composable
fun CosmeticRoleBadge(role: CosmeticRole, onClick: () -> Unit) {
    var image by remember(role.imageUrl) { mutableStateOf<String?>(null) }
    LaunchedEffect(role.imageUrl) {
        image = if (role.imageUrl.isBlank()) null else ServiceLocator.storageRepository.getSignedMediaUrl(role.imageUrl)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(role.color).copy(alpha = 0.14f))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (image != null) {
            AsyncImage(image, role.name, Modifier.size(18.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Text(role.icon, style = MaterialTheme.typography.labelSmall)
        }
        Text(role.name, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun CosmeticRoleReveal(role: CosmeticRole?, onDismiss: () -> Unit) {
    if (role == null) return
    val progress = remember { Animatable(0f) }
    LaunchedEffect(role.id) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(500))
        delay(1100)
        onDismiss()
    }
    val p = progress.value
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = (0.76f * (1f - p * 0.35f)).coerceIn(0f, 0.76f))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var image by remember(role.imageUrl) { mutableStateOf<String?>(null) }
            LaunchedEffect(role.imageUrl) {
                image = if (role.imageUrl.isBlank()) null else ServiceLocator.storageRepository.getSignedMediaUrl(role.imageUrl)
            }
            if (image != null) AsyncImage(image, role.name, Modifier.size((90 + p * 45).dp).clip(CircleShape), contentScale = ContentScale.Crop)
            else Text(role.icon, style = MaterialTheme.typography.displayLarge, color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(role.name, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
    }
}
