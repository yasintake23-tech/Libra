package com.libra.app.feature.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.libra.app.ui.animation.AppleSprings
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.animation.appleGlassSurface
import com.libra.app.ui.animation.appleSquircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.libra.app.core.di.ServiceLocator
import com.libra.app.domain.model.Book
import com.libra.app.ui.components.BookCover
import com.libra.app.ui.components.UserAvatar

/**
 * Dedicated Full-Screen Vertical Reels Book Discovery Section.
 * Designed specifically for Libra's "Kitaplar" section with:
 * - Dynamic blurred book cover ambient background (no flat black screens)
 * - Sharp elevated 3D book cover card
 * - Category, chapter count, and like badges
 * - Full vertical reels interaction column (Like, Comment, Save, Share, Detail)
 * - Prominent "Okumaya Başla" action button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDiscoverySection(
    onStartReading: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onOpenProfile: (String) -> Unit,
    onShareBook: (Book) -> Unit,
    onCreateBookClick: () -> Unit = {},
    topBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BookReelsDiscoverViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        BookReelsFeed(
            state = state,
            onLike = viewModel::toggleLike,
            onSave = viewModel::toggleSave,
            onOpenComments = viewModel::openComments,
            onStartReading = { book ->
                viewModel.startReading(book)
                onStartReading(book)
            },
            onBookClick = onBookClick,
            onOpenProfile = onOpenProfile,
            onShareBook = onShareBook,
            onRetry = viewModel::loadDiscoverBooks,
            onCreateBook = onCreateBookClick
        )

        // Overlay top bar (e.g. section selector)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        ) {
            topBar()
        }

        // Reader Comments Bottom Sheet
        if (state.activeCommentsBookId != null) {
            val bookId = state.activeCommentsBookId!!
            var commentInput by remember { mutableStateOf("") }

            ModalBottomSheet(
                onDismissRequest = { viewModel.closeComments() },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Okur Yorumları (${state.comments.size})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { viewModel.closeComments() }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (state.isLoadingComments) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    } else if (state.comments.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Bu kitap için henüz yorum yapılmamış.\nİlk yorumu sen yaz!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.comments, key = { it.id }) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    UserAvatar(
                                        photoUrl = comment.authorPhotoUrl,
                                        initials = comment.authorName.take(1).uppercase().ifBlank { "L" },
                                        size = 32.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = comment.authorName.ifBlank { "Libra Okuru" },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = comment.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            placeholder = { Text("Yorum ekle...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (commentInput.trim().isNotBlank()) {
                                    val text = commentInput
                                    commentInput = ""
                                    viewModel.addComment(bookId, text)
                                }
                            },
                            enabled = commentInput.trim().isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Send,
                                contentDescription = "Gönder",
                                tint = if (commentInput.trim().isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun BookReelsFeed(
    state: BookReelsState,
    onLike: (Book) -> Unit,
    onSave: (Book) -> Unit,
    onOpenComments: (String) -> Unit,
    onStartReading: (Book) -> Unit,
    onBookClick: (Book) -> Unit,
    onOpenProfile: (String) -> Unit,
    onShareBook: (Book) -> Unit,
    onRetry: () -> Unit,
    onCreateBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.books.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF483D8B).copy(alpha = 0.85f),
                            Color(0xFF282347),
                            Color(0xFF161426)
                        ),
                        radius = 1200f
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Kitaplar Keşfediliyor…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
        return
    }

    if (state.books.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF483D8B).copy(alpha = 0.85f),
                            Color(0xFF282347),
                            Color(0xFF161426)
                        ),
                        radius = 1200f
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Henüz Yayınlanmış Kitap Yok",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "İlk kitabı sen yazarak okurlarla buluşturabilirsin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCreateBook,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("İlk Kitabını Yaz")
                    }
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Yenile")
                    }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { state.books.size })

    VerticalPager(
        state = pagerState,
        beyondViewportPageCount = 1,
        key = { page -> state.books.getOrNull(page)?.id ?: page },
        modifier = modifier.fillMaxSize()
    ) { page ->
        val book = state.books[page]
        val engagement = state.engagements[book.id] ?: BookEngagementUi(likesCount = book.likesCount)
        val previewSnippet = state.chapterPreviews[book.id]
        val previewHasMore = state.chapterPreviewHasMore[book.id] ?: false

        BookReelItem(
            book = book,
            engagement = engagement,
            previewSnippet = previewSnippet,
            previewHasMore = previewHasMore,
            onLike = { onLike(book) },
            onSave = { onSave(book) },
            onOpenComments = { onOpenComments(book.id) },
            onStartReading = { onStartReading(book) },
            onBookClick = { onBookClick(book) },
            onOpenProfile = { onOpenProfile(book.ownerId) },
            onShareBook = { onShareBook(book) }
        )
    }
}

@Composable
private fun BookReelItem(
    book: Book,
    engagement: BookEngagementUi,
    previewSnippet: String?,
    previewHasMore: Boolean = false,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onOpenComments: () -> Unit,
    onStartReading: () -> Unit,
    onBookClick: () -> Unit,
    onOpenProfile: () -> Unit,
    onShareBook: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rawCover = book.displayCoverUrl.trim()
    var signedUrl by remember(rawCover) {
        mutableStateOf<String?>(
            if (rawCover.startsWith("https://") && !rawCover.contains(".r2.dev/")) rawCover else null
        )
    }
    var coverLoadFailed by remember(rawCover) { mutableStateOf(false) }

    LaunchedEffect(rawCover) {
        if (rawCover.isNotBlank()) {
            signedUrl = runCatching {
                ServiceLocator.storageRepository.getSignedMediaUrl(rawCover)
            }.getOrDefault(rawCover)
        } else {
            signedUrl = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Ambient Warm Fallback Gradient (Never pitch black!)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF382F5A),
                            Color(0xFF1D1830),
                            Color(0xFF100E1A)
                        ),
                        radius = 1400f
                    )
                )
        )

        // 2. Full-bleed Sharp Cover Photo (covers 100% of the screen, hardware-accelerated for 120Hz)
        if (!signedUrl.isNullOrBlank() && !coverLoadFailed) {
            val context = LocalContext.current
            val imageRequest = remember(signedUrl) {
                ImageRequest.Builder(context)
                    .data(signedUrl)
                    .crossfade(150)
                    .allowHardware(true)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = book.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { coverLoadFailed = true }
            )
        } else {
            // Elegant watermark when no cover is present or while loading
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 180.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                        tint = Color.White.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            lineHeight = 32.sp
                        ),
                        color = Color.White.copy(alpha = 0.18f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }

        // 3. Immersive Scrim Gradient: subtle top shadow, transparent center for artwork, dark bottom for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.50f),
                        0.18f to Color.Transparent,
                        0.55f to Color.Transparent,
                        0.72f to Color.Black.copy(alpha = 0.50f),
                        0.86f to Color.Black.copy(alpha = 0.85f),
                        1.0f to Color.Black.copy(alpha = 0.98f)
                    )
                )
        )

        // 4. Foreground Content: Badges, Title, Author, Synopsis, Teaser & Start Reading aligned at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 78.dp, bottom = 18.dp)
        ) {
            // Category & Chapter Count Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                ) {
                    Text(
                        text = book.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${book.chapterCount} Bölüm",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )
                    }
                }

                if (engagement.likesCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "${engagement.likesCount}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title
            Text(
                text = book.title.ifBlank { "İsimsiz Kitap" },
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 28.sp
                ),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onBookClick)
            )

            Spacer(Modifier.height(4.dp))

            // Author Row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpenProfile)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    photoUrl = null,
                    initials = book.authorName.take(1).uppercase().ifBlank { "L" },
                    size = 22.dp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = book.authorName.ifBlank { "Yazar" },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }

            // Synopsis
            val synopsis = book.description.ifBlank { book.discoverySummary }
            if (synopsis.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = synopsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Teaser excerpt from Chapter 1 (1,200 - 1,800 chars, paragraph preserved)
            if (!previewSnippet.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.42f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "1. Bölümden Alıntı",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable(onClick = onStartReading)
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Bölümün Devamını Oku",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 110.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            val displayText = if (previewHasMore) "$previewSnippet…" else previewSnippet
                            Text(
                                text = displayText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 17.sp,
                                    letterSpacing = 0.1.sp
                                ),
                                color = Color.White.copy(alpha = 0.94f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Prominent "Okumaya Başla" Action Button
            Button(
                onClick = onStartReading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .appleBouncy(pressedScale = 0.95f, onClick = onStartReading)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Okumaya Başla",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }

        // 5. Right Vertical Action Column (Like, Comment, Save, Share, Detail)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 10.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Author Avatar Button
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .appleBouncy(pressedScale = 0.88f, onClick = onOpenProfile)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    UserAvatar(
                        photoUrl = null,
                        initials = book.authorName.take(1).uppercase().ifBlank { "L" },
                        size = 38.dp
                    )
                }
            }

            // Beğen (Like)
            ReelActionButton(
                icon = if (engagement.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (engagement.likesCount > 0) "${engagement.likesCount}" else "Beğen",
                iconTint = if (engagement.isLiked) Color(0xFFE53935) else Color.White,
                onClick = onLike
            )

            // Yorum (Comment)
            ReelActionButton(
                icon = Icons.Default.ChatBubbleOutline,
                label = if (engagement.commentsCount > 0) "${engagement.commentsCount}" else "Yorum",
                iconTint = Color.White,
                onClick = onOpenComments
            )

            // Kaydet (Save)
            ReelActionButton(
                icon = if (engagement.isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                label = if (engagement.isSaved) "Kaydedildi" else "Kaydet",
                iconTint = if (engagement.isSaved) Color(0xFFFFB300) else Color.White,
                onClick = onSave
            )

            // Paylaş (Share)
            ReelActionButton(
                icon = Icons.Default.Share,
                label = "Paylaş",
                iconTint = Color.White,
                onClick = onShareBook
            )

            // Detay (Detail)
            ReelActionButton(
                icon = Icons.Default.Info,
                label = "Detay",
                iconTint = Color.White,
                onClick = onBookClick
            )
        }
    }
}

@Composable
private fun ReelActionButton(
    icon: ImageVector,
    label: String,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.appleBouncy(pressedScale = 0.86f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Backward compatibility wrapper for callers referencing BookReelsDiscoverScreen.
 */
@Composable
fun BookReelsDiscoverScreen(
    onStartReading: (Book) -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onOpenProfile: (String) -> Unit = {},
    onShareBook: (Book) -> Unit = {},
    modifier: Modifier = Modifier
) {
    BookDiscoverySection(
        onStartReading = onStartReading,
        onBookClick = onBookClick,
        onOpenProfile = onOpenProfile,
        onShareBook = onShareBook,
        modifier = modifier
    )
}
