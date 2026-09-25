package com.libra.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.Story
import com.libra.app.ui.components.UserAvatar
import kotlinx.coroutines.delay

@Composable
fun StoryStrip(
    stories: List<Story>,
    currentUserId: String,
    onStoryClick: (Story) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestByAuthor = stories
        .groupBy { it.authorId }
        .mapValues { (_, values) -> values.maxByOrNull { it.createdAt }!! }
        .values
        .sortedWith(compareByDescending<Story> { it.authorId == currentUserId }.thenByDescending { it.createdAt })

    if (latestByAuthor.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp)
    ) {
        items(latestByAuthor.toList(), key = { it.authorId }) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(66.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .border(2.5.dp, Color.White, CircleShape)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .clickable { onStoryClick(story) },
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        photoUrl = story.authorPhotoUrl,
                        initials = story.authorName.take(1).uppercase().ifBlank { "L" },
                        size = 53.dp
                    )
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    if (story.authorId == currentUserId) "Sen" else story.authorName.ifBlank { "Libra" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun StoryViewer(
    story: Story,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onLike: () -> Unit
) {
    var mediaUrl by remember(story.id, story.mediaUrl) { mutableStateOf("") }
    var liked by remember(story.id) { mutableStateOf(false) }
    val progress = remember(story.id) { Animatable(0f) }

    LaunchedEffect(story.id) {
        mediaUrl = ""
        progress.snapTo(0f)
        if (story.mediaUrl.isNotBlank()) {
            mediaUrl = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(story.mediaUrl)
            }.getOrElse { story.mediaUrl }
        }
        progress.animateTo(
            1f,
            animationSpec = tween(6000, easing = FastOutSlowInEasing)
        )
        onDismiss()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (mediaUrl.isNotBlank()) {
            AsyncImage(
                model = mediaUrl,
                contentDescription = "Hikâye",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
        } else if (story.text.isNotBlank()) {
            Text(
                story.text,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserAvatar(
                photoUrl = story.authorPhotoUrl,
                initials = story.authorName.take(1).uppercase().ifBlank { "L" },
                size = 38.dp
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(story.authorName.ifBlank { "Libra kullanıcısı" }, color = Color.White)
                Text("24 saatlik hikâye", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, "Kapat", tint = Color.White)
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .align(Alignment.TopCenter)
                .padding(top = 5.dp),
            color = Color.Transparent
        ) {
            androidx.compose.material3.LinearProgressIndicator(
                progress = { progress.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .align(Alignment.BottomCenter),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onReply),
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Reply, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Yanıtla", color = Color.White.copy(alpha = 0.82f))
                }
            }
            IconButton(onClick = {
                liked = !liked
                onLike()
            }) {
                Icon(
                    Icons.Default.Favorite,
                    "Beğen",
                    tint = if (liked) Color(0xFFFF4D6D) else Color.White
                )
            }
        }

        if (story.text.isNotBlank() && mediaUrl.isNotBlank()) {
            Text(
                story.text,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 22.dp, end = 76.dp, bottom = 92.dp),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
