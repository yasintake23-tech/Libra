package com.libra.app.feature.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator

@Composable
internal fun AdminSignedImage(
    mediaKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    contentScale: ContentScale = ContentScale.Crop,
    onImageClick: ((resolvedSignedUrl: String) -> Unit)? = null
) {
    if (mediaKey.isBlank()) return

    var resolvedUrl by remember(mediaKey) { mutableStateOf<String?>(null) }
    var imageFailed by remember(mediaKey) { mutableStateOf(false) }

    LaunchedEffect(mediaKey) {
        imageFailed = false
        resolvedUrl = null
        val signed = runCatching {
            ServiceLocator.storageRepository.getSignedMediaUrl(mediaKey)
        }.getOrNull()
        if (signed.isNullOrBlank()) {
            imageFailed = true
        } else {
            resolvedUrl = signed
        }
    }

    when {
        imageFailed -> {
            Surface(
                modifier = modifier.clip(shape),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrokenImage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Fotoğraf yüklenemedi",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        resolvedUrl == null -> {
            Surface(
                modifier = modifier.clip(shape),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Fotoğraf hazırlanıyor…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        else -> {
            val signed = resolvedUrl!!
            AsyncImage(
                model = signed,
                contentDescription = contentDescription,
                contentScale = contentScale,
                onError = { imageFailed = true },
                modifier = modifier
                    .clip(shape)
                    .then(
                        if (onImageClick != null) {
                            Modifier.clickable { onImageClick(signed) }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
