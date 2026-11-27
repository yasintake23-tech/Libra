package com.libra.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent

@Composable
fun FullScreenImageViewerDialog(
    mediaUrl: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (mediaUrl.isBlank()) return

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var dismissDragY by remember { mutableFloatStateOf(0f) }

        val animatedScale by animateFloatAsState(targetValue = scale, label = "scaleAnim")

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.96f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1.05f) {
                                scale = 1f
                                offset = Offset.Zero
                                dismissDragY = 0f
                            } else {
                                scale = 2.5f
                                dismissDragY = 0f
                            }
                        },
                        onTap = {
                            // Don't propagate clicks to post/message underneath
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale

                        if (scale > 1.05f) {
                            // Pan inside the zoomed image
                            val maxOffsetX = (size.width * (scale - 1f)) / 2f
                            val maxOffsetY = (size.height * (scale - 1f)) / 2f
                            offset = Offset(
                                x = (offset.x + pan.x).coerceIn(-maxOffsetX, maxOffsetX),
                                y = (offset.y + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                            )
                        } else {
                            // Scale is 1x: Swipe down to dismiss
                            offset = Offset.Zero
                            if (pan.y > 0 || dismissDragY > 0) {
                                dismissDragY += pan.y
                                if (dismissDragY > 220f) {
                                    onDismiss()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = mediaUrl,
                contentDescription = "Tam Ekran Görsel",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        translationX = offset.x
                        translationY = offset.y + dismissDragY
                    },
                contentScale = ContentScale.Fit
            ) {
                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Görsel yüklenemedi",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    else -> {
                        SubcomposeAsyncImageContent()
                    }
                }
            }

            // Close button overlay
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.5f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
