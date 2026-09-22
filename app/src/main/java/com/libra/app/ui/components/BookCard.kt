package com.libra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.libra.app.data.storage.CloudflareR2StorageConfig
import com.libra.app.domain.model.Book

@Composable
fun BookCover(book: Book, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (book.coverImageUrl.isNotBlank()) {
            val model: Any = if (CloudflareR2StorageConfig.isObjectApiUrl(book.coverImageUrl)) {
                ImageRequest.Builder(context)
                    .data(book.coverImageUrl)
                    .apply {
                        val token = CloudflareR2StorageConfig.apiToken(context)
                        if (token.isNotBlank()) {
                            addHeader("Authorization", "Bearer " + token)
                        }
                    }
                    .build()
            } else {
                book.coverImageUrl
            }

            AsyncImage(
                model = model,
                contentDescription = book.title + " kapak",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                Icons.Default.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun VerticalBookCard(book: Book, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.width(126.dp).clickable(onClick = onClick).testTag("book_card_" + book.id),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(7.dp)) {
            BookCover(book, Modifier.fillMaxWidth().aspectRatio(.68f))
            Spacer(Modifier.height(8.dp))
            Text(book.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(book.authorName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(String.format("%.1f", book.rating), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun HorizontalBookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).testTag("horizontal_book_card_" + book.id),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.width(58.dp).aspectRatio(.68f))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.authorName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (book.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(book.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                extraContent?.let {
                    Spacer(Modifier.height(6.dp))
                    it()
                }
            }
        }
    }
}
