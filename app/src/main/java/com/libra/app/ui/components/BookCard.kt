package com.libra.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookStatus

/**
 * Aesthetic Book Cover component.
 * If cover image exists, loads from CDN; otherwise displays a beautifully styled
 * minimalist typographic book cover with subtle leather/paper gradient and category badge.
 */
/**
 * Shared Premium Book Cover Component.
 * Supports Cloudflare R2 signed URL resolution (identical to posts/DMs),
 * smooth image loading with Coil, error handling, and elegant fallback cover.
 */
@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    contentScale: ContentScale = ContentScale.Crop
) {
    BookCover(
        coverUrlOrKey = book.displayCoverUrl,
        title = book.title,
        authorName = book.authorName,
        categoryName = book.category.displayName,
        modifier = modifier,
        cornerRadius = cornerRadius,
        contentScale = contentScale
    )
}

@Composable
fun BookCover(
    coverUrlOrKey: String,
    title: String = "",
    authorName: String = "",
    categoryName: String = "",
    modifier: Modifier = Modifier,
    cornerRadius: Int = 12,
    contentScale: ContentScale = ContentScale.Crop
) {
    val rawCover = coverUrlOrKey.trim()
    val shape = RoundedCornerShape(cornerRadius.dp)

    var signedUrl by remember(rawCover) {
        mutableStateOf<String?>(
            if (rawCover.startsWith("https://") && !rawCover.contains(".r2.dev/")) rawCover else null
        )
    }
    var loadFailed by remember(rawCover) { mutableStateOf(false) }

    LaunchedEffect(rawCover) {
        if (rawCover.isNotBlank()) {
            signedUrl = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(rawCover)
            }.getOrDefault(rawCover)
        } else {
            signedUrl = null
        }
    }

    Box(
        modifier = modifier
            .shadow(4.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!signedUrl.isNullOrBlank() && !loadFailed) {
            AsyncImage(
                model = signedUrl,
                contentDescription = if (title.isNotBlank()) "$title kapak" else "Kitap kapağı",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
                onError = {
                    loadFailed = true
                }
            )
        } else {
            // Elegant fallback cover (Libra signature typography & soft leather/paper aesthetic)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        RoundedCornerShape((cornerRadius - 2).coerceAtLeast(4).dp)
                    )
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = categoryName.ifBlank { "Libra" },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = title.ifBlank { "İsimsiz Kitap" },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = authorName.ifBlank { "Libra Yazarı" },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Premium Vertical Book Card for feeds, search, and category rows.
 */
@Composable
fun VerticalBookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReadClick: (() -> Unit)? = null,
    progressPercent: Int? = null
) {
    Card(
        modifier = modifier
            .width(150.dp)
            .clickable(onClick = onClick)
            .testTag("book_card_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Book cover
            BookCover(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f),
                cornerRadius = 10
            )

            Spacer(Modifier.height(8.dp))

            // Title & Author
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = book.authorName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(6.dp))

            // Metrics row: Likes / Rating + Chapter count
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${book.likesCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${if (book.chapterCount > 0) book.chapterCount else 1} Bölüm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Progress bar if in reading list
            if (progressPercent != null && progressPercent > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Action button
            if (onReadClick != null) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onReadClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (progressPercent != null && progressPercent > 0) "Devam Et" else "Oku",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * Premium Horizontal Book Card with detailed summary, tags, and actions.
 */
@Composable
fun HorizontalBookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onReadClick: (() -> Unit)? = null,
    progressPercent: Int? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("horizontal_book_card_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                book = book,
                modifier = Modifier
                    .width(72.dp)
                    .aspectRatio(0.68f),
                cornerRadius = 10
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = book.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (book.status == BookStatus.PUBLISHED) {
                        Text(
                            text = "${if (book.chapterCount > 0) book.chapterCount else 1} Bölüm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Taslak",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.authorName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (book.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (progressPercent != null && progressPercent > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { progressPercent / 100f },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "%$progressPercent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                extraContent?.let {
                    Spacer(Modifier.height(6.dp))
                    it()
                }

                if (onReadClick != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onReadClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (progressPercent != null && progressPercent > 0) "Devam Et" else "Kitabı Oku",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Featured Hero Book Card for Discover / Carousel sections.
 * Features an ambient matching background and quick read action.
 */
@Composable
fun FeaturedDiscoverBookCard(
    book: Book,
    onClick: () -> Unit,
    onReadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BookCover(
                    book = book,
                    modifier = Modifier
                        .width(96.dp)
                        .aspectRatio(0.68f),
                    cornerRadius = 12
                )

                Spacer(Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = book.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = book.authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = "${book.likesCount}", style = MaterialTheme.typography.labelSmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(text = "${book.readCount}", style = MaterialTheme.typography.labelSmall)
                        }

                        Text(
                            text = "${if (book.chapterCount > 0) book.chapterCount else 1} Bölüm",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (book.discoverySummary.isNotBlank() || book.description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = book.discoverySummary.ifBlank { book.description },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Detaylar")
                }

                Button(
                    onClick = onReadClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Kitabı Oku")
                }
            }
        }
    }
}
