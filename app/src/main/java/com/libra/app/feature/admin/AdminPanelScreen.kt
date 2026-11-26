package com.libra.app.feature.admin

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.libra.app.BuildConfig
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.*
import com.libra.app.feature.book.BookDetailScreen
import com.libra.app.feature.book.manage.BookManagementRootScreen
import com.libra.app.feature.book.manage.ManagementSubPage
import com.libra.app.feature.reader.BookReaderScreen
import com.libra.app.feature.write.create.CreateBookScreen
import com.libra.app.ui.animation.appleBouncy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

private val sections = listOf(
    "Üye işlemleri",
    "Kitap işlemleri",
    "Gönderi işlemleri",
    "Hikâye işlemleri",
    "Sunucu işlemleri",
    "Genel chat işlemleri",
    "Güncelleme ayarları"
)

@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    permissions: AdminPermissionSet = AdminPermissionSet(
        manageMembers = true,
        manageBans = true,
        editProfiles = true,
        manageBooks = true,
        managePosts = true,
        manageStories = true,
        manageServers = true,
        manageGlobalChat = true,
        manageAdminRoles = true,
        manageCosmetics = true,
        sendFeedback = true,
        manageUpdates = true
    ),
    onOpenProfile: ((String) -> Unit)? = null,
    vm: AdminViewModel = viewModel()
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val currentUid = remember {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        }
        val members by vm.members.collectAsState()
        val books by vm.books.collectAsState()
        val posts by vm.posts.collectAsState()
        val stories by vm.stories.collectAsState()
        val servers by vm.servers.collectAsState()
        val globalMessages by vm.globalMessages.collectAsState()

        var section by remember { mutableStateOf<String?>(null) }
        var member by remember { mutableStateOf<UserProfile?>(null) }
        var booksOwnerFilter by remember { mutableStateOf<UserProfile?>(null) }
        var postsOwnerFilter by remember { mutableStateOf<UserProfile?>(null) }
        var storiesOwnerFilter by remember { mutableStateOf<UserProfile?>(null) }
        var serversOwnerFilter by remember { mutableStateOf<UserProfile?>(null) }
        var globalChatOwnerFilter by remember { mutableStateOf<UserProfile?>(null) }
        var controlBook by remember { mutableStateOf<Book?>(null) }
        var bookSubModule by remember { mutableStateOf<ManagementSubPage?>(null) }
        var previewBook by remember { mutableStateOf<Book?>(null) }
        var previewReaderBook by remember { mutableStateOf<Book?>(null) }
        var previewReaderChapterId by remember { mutableStateOf<String?>(null) }
        var showCreateBook by remember { mutableStateOf(false) }

        BackHandler {
            when {
                previewReaderBook != null -> {
                    previewReaderBook = null
                    previewReaderChapterId = null
                }
                previewBook != null -> previewBook = null
                bookSubModule != null -> bookSubModule = null
                controlBook != null -> controlBook = null
                showCreateBook -> showCreateBook = false
                section == "Kitap işlemleri" -> {
                    section = null
                    booksOwnerFilter = null
                }
                section == "Gönderi işlemleri" -> {
                    section = null
                    postsOwnerFilter = null
                }
                section == "Hikâye işlemleri" -> {
                    section = null
                    storiesOwnerFilter = null
                }
                section == "Sunucu işlemleri" -> {
                    section = null
                    serversOwnerFilter = null
                }
                section == "Genel chat işlemleri" -> {
                    section = null
                    globalChatOwnerFilter = null
                }
                section == "Güncelleme ayarları" -> section = null
                member != null -> member = null
                section != null -> section = null
                else -> onBack()
            }
        }

        when {
            previewReaderBook != null -> {
                BookReaderScreen(
                    book = previewReaderBook!!,
                    userId = currentUid,
                    initialChapterId = previewReaderChapterId,
                    onBack = {
                        previewReaderBook = null
                        previewReaderChapterId = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            previewBook != null -> {
                BookDetailScreen(
                    book = previewBook!!,
                    userId = currentUid,
                    onBack = { previewBook = null },
                    onRead = { selected, chapter ->
                        previewReaderBook = selected
                        previewReaderChapterId = chapter?.id
                    },
                    onOpenChapter = { selected, chapter ->
                        previewReaderBook = selected
                        previewReaderChapterId = chapter.id
                    },
                    onOpenAuthor = onOpenProfile,
                    modifier = Modifier.fillMaxSize()
                )
            }
            controlBook != null && bookSubModule != null -> {
                BookManagementRootScreen(
                    book = controlBook!!,
                    onBack = { bookSubModule = null },
                    onPreviewBook = { target -> previewBook = target },
                    onDeleteBook = { target ->
                        vm.deleteBookAsAdmin(target.id) { ok ->
                            if (ok) {
                                bookSubModule = null
                                controlBook = null
                            }
                        }
                    },
                    onOpenProfile = onOpenProfile,
                    initialSubPage = bookSubModule!!,
                    modifier = Modifier.fillMaxSize()
                )
            }
            controlBook != null -> {
                val activeBook = books.firstOrNull { it.id == controlBook!!.id } ?: controlBook!!
                val ownerProfile = members.firstOrNull { it.uid == activeBook.ownerId }
                AdminBookControlCenter(
                    book = activeBook,
                    ownerProfile = ownerProfile,
                    vm = vm,
                    permissions = permissions,
                    onBack = { controlBook = null },
                    onOpenSubModule = { subPage -> bookSubModule = subPage },
                    onPreviewBook = { target -> previewBook = target },
                    onOpenMemberAdmin = { targetMember ->
                        controlBook = null
                        bookSubModule = null
                        booksOwnerFilter = null
                        section = null
                        member = targetMember
                    },
                    onFilterAuthorBooks = { ownerUid ->
                        controlBook = null
                        bookSubModule = null
                        booksOwnerFilter = members.firstOrNull { it.uid == ownerUid }
                            ?: UserProfile(uid = ownerUid, displayName = activeBook.authorName)
                        section = "Kitap işlemleri"
                    },
                    onDeleted = {
                        controlBook = null
                        bookSubModule = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            showCreateBook -> {
                CreateBookScreen(
                    onBack = { showCreateBook = false },
                    onBookCreated = { newBook ->
                        showCreateBook = false
                        controlBook = newBook
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Kitap işlemleri" -> {
                AdminBooksScreen(
                    vm = vm,
                    permissions = permissions,
                    initialOwnerFilter = booksOwnerFilter,
                    onBack = {
                        section = null
                        booksOwnerFilter = null
                    },
                    onOpenBookManagement = { book, subPage ->
                        controlBook = book
                        bookSubModule = if (subPage == ManagementSubPage.HUB) null else subPage
                    },
                    onPreviewBook = { book -> previewBook = book },
                    onCreateBook = { showCreateBook = true },
                    onOpenMemberAdmin = { owner ->
                        section = null
                        booksOwnerFilter = null
                        member = owner
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Gönderi işlemleri" -> {
                AdminPostsScreen(
                    vm = vm,
                    permissions = permissions,
                    initialOwnerFilter = postsOwnerFilter,
                    onBack = {
                        section = null
                        postsOwnerFilter = null
                    },
                    onOpenMemberAdmin = { author ->
                        section = null
                        postsOwnerFilter = null
                        member = author
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Hikâye işlemleri" -> {
                AdminStoriesScreen(
                    vm = vm,
                    permissions = permissions,
                    initialOwnerFilter = storiesOwnerFilter,
                    onBack = {
                        section = null
                        storiesOwnerFilter = null
                    },
                    onOpenMemberAdmin = { author ->
                        section = null
                        storiesOwnerFilter = null
                        member = author
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Sunucu işlemleri" -> {
                AdminServersScreen(
                    vm = vm,
                    permissions = permissions,
                    initialOwnerFilter = serversOwnerFilter,
                    onBack = {
                        section = null
                        serversOwnerFilter = null
                    },
                    onOpenMemberAdmin = { owner ->
                        section = null
                        serversOwnerFilter = null
                        member = owner
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Genel chat işlemleri" -> {
                AdminGlobalChatScreen(
                    vm = vm,
                    permissions = permissions,
                    initialOwnerFilter = globalChatOwnerFilter,
                    onBack = {
                        section = null
                        globalChatOwnerFilter = null
                    },
                    onOpenMemberAdmin = { sender ->
                        section = null
                        globalChatOwnerFilter = null
                        member = sender
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Güncelleme ayarları" -> {
                UpdateSettingsPage(vm, { section = null }, Modifier.fillMaxSize())
            }
            member != null -> {
                MemberAdminScreen(
                    member = member!!,
                    vm = vm,
                    onBack = { member = null },
                    permissions = permissions,
                    onOpenMemberBooks = { target ->
                        booksOwnerFilter = target
                        section = "Kitap işlemleri"
                    },
                    onOpenMemberPosts = { target ->
                        postsOwnerFilter = target
                        section = "Gönderi işlemleri"
                    },
                    onOpenMemberStories = { target ->
                        storiesOwnerFilter = target
                        section = "Hikâye işlemleri"
                    },
                    onOpenMemberServers = { target ->
                        serversOwnerFilter = target
                        section = "Sunucu işlemleri"
                    },
                    onOpenMemberGlobalChat = { target ->
                        globalChatOwnerFilter = target
                        section = "Genel chat işlemleri"
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            section == "Üye işlemleri" -> {
                MembersScreen(members, { section = null }, { member = it }, Modifier.fillMaxSize(), permissions.manageMembers)
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                            ) {
                                Icon(Icons.Default.ArrowBack, "Geri")
                            }
                            Column {
                                Text(
                                    "Yönetim",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "Libra yönetim merkezi",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                    }

                    items(sections, key = { it }) { title ->
                        val enabled = when (title) {
                            "Üye işlemleri" -> permissions.manageMembers
                            "Kitap işlemleri" -> permissions.manageBooks
                            "Gönderi işlemleri" -> permissions.managePosts
                            "Hikâye işlemleri" -> permissions.manageStories
                            "Sunucu işlemleri" -> permissions.manageServers
                            "Genel chat işlemleri" -> permissions.manageGlobalChat
                            "Güncelleme ayarları" -> permissions.manageUpdates
                            else -> false
                        }
                        val (subtitle, icon) = when (title) {
                            "Üye işlemleri" -> "${members.size} üye • Ban, rol, profil ve kitap yetkileri" to Icons.Default.People
                            "Kitap işlemleri" -> "${books.size} kitap • Kategorizasyon, yayın, bölüm ve moderasyon" to Icons.AutoMirrored.Filled.MenuBook
                            "Gönderi işlemleri" -> "${posts.size} gönderi • Akış, etiket, yorum ve üye yetkileri" to Icons.Default.Edit
                            "Hikâye işlemleri" -> "${stories.size} hikâye • 24 saat akış, medya ve hikâye izinleri" to Icons.Default.ChatBubbleOutline
                            "Sunucu işlemleri" -> "${servers.size} sunucu • Kanallar, mesajlar, üyeler ve yasaklar" to Icons.Default.Groups
                            "Genel chat işlemleri" -> "${globalMessages.size} mesaj • Akış denetimi, duyuru ve mesaj izinleri" to Icons.Default.ChatBubbleOutline
                            "Güncelleme ayarları" -> "APK yayınlama ve sürüm yönetimi" to Icons.Default.SystemUpdate
                            else -> "" to Icons.Default.Settings
                        }

                        val shape = RoundedCornerShape(18.dp)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .appleBouncy(enabled = enabled, pressedScale = 0.97f, onClick = {
                                    if (enabled) {
                                        booksOwnerFilter = null
                                        postsOwnerFilter = null
                                        storiesOwnerFilter = null
                                        serversOwnerFilter = null
                                        globalChatOwnerFilter = null
                                        section = title
                                    }
                                }),
                            shape = shape,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (enabled) subtitle else "Bu alan için yetkin yok",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateSettingsPage(vm: AdminViewModel, onBack: () -> Unit, modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resolver = context.contentResolver
    val clipboard = LocalClipboardManager.current
    val active = vm.activeRelease.collectAsState().value
    val detected = vm.selectedReleaseInfo.collectAsState().value
    val progress = vm.releaseProgress.collectAsState().value
    val busy = vm.releaseBusy.collectAsState().value
    val status = vm.releaseStatus.collectAsState().value

    var releaseId by remember { mutableStateOf("") }
    var versionName by remember { mutableStateOf(BuildConfig.VERSION_NAME) }
    var versionCode by remember { mutableStateOf(BuildConfig.VERSION_CODE.toString()) }
    var changelog by remember { mutableStateOf("") }
    var forceUpdate by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf(0L) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
        selectedName = ""
        selectedSize = 0L
        vm.clearSelectedRelease()
        if (uri == null) return@rememberLauncherForActivityResult

        selectedName = uri.lastPathSegment?.substringAfterLast("/")?.ifBlank { "Libra-release.apk" }
            ?: "Libra-release.apk"
        selectedSize = runCatching {
            resolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            } ?: 0L
        }.getOrDefault(0L)

        vm.inspectAppRelease(uri)
    }

    LaunchedEffect(detected?.releaseId) {
        detected?.releaseId?.let { releaseId = it }
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy) {
                Icon(Icons.Default.ArrowBack, "Geri")
            }
            Column {
                Text(
                    "Güncelleme ayarları",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text("APK yayınlama ve sürüm yönetimi", color = Color.Gray)
            }
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Mevcut uygulama", fontWeight = FontWeight.Bold)
                        Text("Kurulu sürüm: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                        Text("Kurulu release ID: ${BuildConfig.LIBRA_RELEASE_ID}", color = Color.Gray)
                        Text("Aktif yayın: " + (active?.versionName ?: "Henüz yayınlanmadı"))
                        Text("Aktif Release ID: " + (active?.releaseId ?: "Henüz yok"), color = Color.Gray)
                        active?.releaseId?.takeIf { it.isNotBlank() }?.let { id ->
                            OutlinedButton(onClick = { clipboard.setText(AnnotatedString(id)) }) {
                                Text("Release IDyi kopyala")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Yeni yayın",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Button(
                    onClick = {
                        picker.launch(
                            arrayOf(
                                "application/vnd.android.package-archive",
                                "application/octet-stream"
                            )
                        )
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedName.isBlank()) "APK seç" else "APK: $selectedName")
                }

                if (selectedSize > 0L) {
                    Text(
                        "Boyut: %.1f MB".format(selectedSize / 1024f / 1024f),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = releaseId,
                    onValueChange = { releaseId = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Release ID") },
                    placeholder = { Text("APK seçildiğinde otomatik gelir") },
                    singleLine = true,
                    enabled = false
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = versionName,
                        onValueChange = { versionName = it.take(30) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Sürüm adı") },
                        enabled = !busy
                    )
                    OutlinedTextField(
                        value = versionCode,
                        onValueChange = { versionCode = it.filter(Char::isDigit).take(10) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Version code") },
                        enabled = !busy
                    )
                }
            }

            item {
                if (detected != null) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("APK doğrulandı", fontWeight = FontWeight.Bold)
                            Text("APK Release ID: ${detected.releaseId}")
                            Text(
                                "APK'nın package ve imzası doğrulandı. Sürüm adı ve version code'u aşağıdan sen belirleyebilirsin.",
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = changelog,
                    onValueChange = { changelog = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Değişiklik notları") },
                    minLines = 4,
                    enabled = !busy
                )
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Zorunlu güncelleme", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Eski sürümlerde uygulamayı güncelleme ekranında tutar.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(forceUpdate, { forceUpdate = it }, enabled = !busy)
                }
            }

            item {
                if (busy) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("APK yükleniyor: %$progress")
                }
                status?.let {
                    Text(
                        it,
                        color = if (
                            it.startsWith("Yeni sürüm") ||
                            it.contains("otomatik algılandı")
                        ) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }

            item {
                val code = versionCode.toLongOrNull() ?: 0L
                val canPublish = !busy &&
                    selectedUri != null &&
                    selectedSize > 0L &&
                    detected != null &&
                    releaseId.trim() == detected.releaseId &&
                    code > maxOf(
                        BuildConfig.VERSION_CODE.toLong(),
                        active?.versionCode ?: 0L
                    ) &&
                    versionName.trim().isNotBlank()

                Button(
                    onClick = {
                        val uri = selectedUri ?: return@Button
                        vm.publishAppRelease(
                            uri = uri,
                            fileName = selectedName,
                            fileSize = selectedSize,
                            releaseId = releaseId.trim(),
                            versionCode = code,
                            versionName = versionName.trim(),
                            changelog = changelog.lines()
                                .map { it.trim() }
                                .filter { it.isNotBlank() },
                            forceUpdate = forceUpdate
                        )
                    },
                    enabled = canPublish,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (busy) "Yayınlanıyor..." else "Yayınla")
                }
            }
        }
    }
}

@Composable private fun MembersScreen(members: List<UserProfile>, onBack: () -> Unit, onSelect: (UserProfile) -> Unit, modifier: Modifier, enabled: Boolean) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Tümü") }
    val now = System.currentTimeMillis()
    val list = members.filter { u ->
        val q = query.trim().lowercase()
        val match = q.isBlank() || u.displayName.lowercase().contains(q) || u.username.lowercase().contains(q) || u.email.lowercase().contains(q)
        val date = when(filter) {
            "Bugün" -> u.createdAt >= now - 86400000L
            "7 gün" -> u.createdAt >= now - 7L*86400000L
            "30 gün" -> u.createdAt >= now - 30L*86400000L
            else -> true
        }
        match && date
    }
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Text("Üye işlemleri", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        }
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("İsim, kullanıcı adı veya e-posta ara") })
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Tümü","Bugün","7 gün","30 gün").forEach { FilterChip(filter == it, { filter = it }, label = { Text(it) }) }
        }
        Text(list.size.toString() + " üye", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.uid }) { u ->
                val shape = RoundedCornerShape(16.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .appleBouncy(pressedScale = 0.98f, onClick = { onSelect(u) }),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(u.displayName.ifBlank { "İsimsiz üye" }, fontWeight = FontWeight.SemiBold)
                            Text("@" + u.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(u.email, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                        if (u.moderation.isBanned) Text("BAN", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberAdminScreen(
    member: UserProfile,
    vm: AdminViewModel,
    onBack: () -> Unit,
    permissions: AdminPermissionSet,
    onOpenMemberBooks: (UserProfile) -> Unit,
    onOpenMemberPosts: (UserProfile) -> Unit,
    onOpenMemberStories: (UserProfile) -> Unit,
    onOpenMemberServers: (UserProfile) -> Unit,
    onOpenMemberGlobalChat: (UserProfile) -> Unit,
    modifier: Modifier
) {
    var current by remember(member) { mutableStateOf(member) }
    var page by remember { mutableStateOf<String?>(null) }

    if (page == "Ban ve erişim işlemleri") {
        ModerationPage(current, { page = null }) {
            current = current.copy(moderation = it)
            vm.saveModeration(current.uid, it)
            page = null
        }
        return
    }
    if (page == "Yönetici rolü verme işlemleri") {
        AdminRolePage(current.uid, vm, { page = null })
        return
    }
    if (page == "Kozmetik rol") {
        CosmeticPage(current, vm, { page = null })
        return
    }

    val actions = listOf(
        "Ban ve erişim işlemleri" to permissions.manageBans,
        "Hesap ve üyelik işlemleri" to permissions.editProfiles,
        "Kitap işlemleri" to permissions.manageBooks,
        "Gönderi işlemleri" to permissions.managePosts,
        "Hikâye işlemleri" to permissions.manageStories,
        "Sunucu işlemleri" to permissions.manageServers,
        "Genel chat işlemleri" to permissions.manageGlobalChat,
        "Yönetici rolü verme işlemleri" to permissions.manageAdminRoles,
        "Kozmetik rol" to permissions.manageCosmetics,
        "Kullanıcıya geri bildirim / DM" to permissions.sendFeedback
    ).filter { it.second }

    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
                    Column {
                        Text(current.displayName, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Text("@" + current.username, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
            items(actions, key = { it.first }) { (title, _) ->
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(shape)
                        .appleBouncy(pressedScale = 0.97f, onClick = {
                            when (title) {
                                "Kitap işlemleri" -> onOpenMemberBooks(current)
                                "Gönderi işlemleri" -> onOpenMemberPosts(current)
                                "Hikâye işlemleri" -> onOpenMemberStories(current)
                                "Sunucu işlemleri" -> onOpenMemberServers(current)
                                "Genel chat işlemleri" -> onOpenMemberGlobalChat(current)
                                else -> page = title
                            }
                        }),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    when (page) {
        "Hesap ve üyelik işlemleri" -> ProfileDialog(current, { page = null }) { current = it; vm.saveProfile(it) }
        "Kullanıcıya geri bildirim / DM" -> ContactDialog(current, { page = null })
    }
}

@Composable
private fun ModerationPage(member: UserProfile, onBack: () -> Unit, onSave: (UserModeration) -> Unit) {
    var value by remember(member.uid) { mutableStateOf(member.moderation) }
    var reason by remember(member.uid) { mutableStateOf(member.moderation.banReason) }
    var duration by remember(member.uid) { mutableStateOf("Kalıcı") }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column {
                Text("Ban ve erişim işlemleri", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(member.displayName.ifBlank { "Üye" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item {
                OutlinedTextField(reason, { reason = it }, Modifier.fillMaxWidth(), label = { Text("Ban sebebi") }, minLines = 2)
                Spacer(Modifier.height(10.dp))
                Text("Ban süresi", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    listOf("1 saat", "1 gün", "7 gün", "Kalıcı").forEach {
                        FilterChip(duration == it, { duration = it }, label = { Text(it) })
                    }
                }
            }
            item { SwitchRow("Ban", value.banned) { value = value.copy(banned = it) } }
            item { SwitchRow("Gönderi paylaşma engeli", value.postingDisabled) { value = value.copy(postingDisabled = it) } }
            item { SwitchRow("Mesaj atma engeli", value.messagingDisabled) { value = value.copy(messagingDisabled = it) } }
            item { SwitchRow("Hikâye paylaşma engeli", value.storyDisabled) { value = value.copy(storyDisabled = it) } }
            item { SwitchRow("Yorum yapma engeli", value.commentingDisabled) { value = value.copy(commentingDisabled = it) } }
            item { SwitchRow("Kitap paylaşma engeli", value.bookPublishingDisabled) { value = value.copy(bookPublishingDisabled = it) } }
            item { SwitchRow("Sunucu oluşturma engeli", value.serverCreationDisabled) { value = value.copy(serverCreationDisabled = it) } }
            item { SwitchRow("Sunucu/genel chat mesaj engeli", value.serverMessagingDisabled) { value = value.copy(serverMessagingDisabled = it) } }
            item {
                Spacer(Modifier.height(16.dp))
                Button(onClick = {
                    val until = when (duration) {
                        "1 saat" -> System.currentTimeMillis() + 3_600_000L
                        "1 gün" -> System.currentTimeMillis() + 86_400_000L
                        "7 gün" -> System.currentTimeMillis() + 7L * 86_400_000L
                        else -> 0L
                    }
                    onSave(value.copy(banReason = reason.trim(), banUntil = if (value.banned) until else 0L))
                }, modifier = Modifier.fillMaxWidth()) { Text("Değişiklikleri kaydet") }
            }
        }
    }
}

@Composable private fun ModerationDialog(m: UserModeration, dismiss:()->Unit, save:(UserModeration)->Unit) {
    var x by remember { mutableStateOf(m) }
    var reason by remember { mutableStateOf(m.banReason) }
    var duration by remember { mutableStateOf("Kalıcı") }
    AlertDialog(onDismissRequest=dismiss,title={Text("Ban ve erişim işlemleri")},text={ Column(Modifier.heightIn(max=560.dp)) {
        OutlinedTextField(reason,{reason=it},Modifier.fillMaxWidth(),label={Text("Ban sebebi")})
        Text("Ban süresi", fontWeight=FontWeight.Bold, modifier=Modifier.padding(top=8.dp))
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf("1 saat","1 gün","7 gün","Kalıcı").forEach { FilterChip(duration == it, { duration = it }, label = { Text(it) }) } }
        SwitchRow("Ban",x.banned){x=x.copy(banned=it)}
        SwitchRow("Gönderi paylaşma engeli",x.postingDisabled){x=x.copy(postingDisabled=it)}
        SwitchRow("Mesaj atma engeli",x.messagingDisabled){x=x.copy(messagingDisabled=it)}
        SwitchRow("Hikâye paylaşma engeli",x.storyDisabled){x=x.copy(storyDisabled=it)}
        SwitchRow("Yorum yapma engeli",x.commentingDisabled){x=x.copy(commentingDisabled=it)}
        SwitchRow("Kitap paylaşma engeli",x.bookPublishingDisabled){x=x.copy(bookPublishingDisabled=it)}
        SwitchRow("Sunucu oluşturma engeli",x.serverCreationDisabled){x=x.copy(serverCreationDisabled=it)}
        SwitchRow("Sunucu/genel chat mesaj engeli",x.serverMessagingDisabled){x=x.copy(serverMessagingDisabled=it)}
    }},confirmButton={TextButton({val until=when(duration){"1 saat"->System.currentTimeMillis()+3600000L;"1 gün"->System.currentTimeMillis()+86400000L;"7 gün"->System.currentTimeMillis()+7L*86400000L;else->0L};save(x.copy(banReason=reason,banUntil=if(x.banned)until else 0L));dismiss()}){Text("Kaydet")}},dismissButton={TextButton(dismiss){Text("İptal")}})
}

@Composable private fun SwitchRow(t:String,c:Boolean,change:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth(),Arrangement.SpaceBetween,Alignment.CenterVertically){Text(t,Modifier.weight(1f));Switch(c,change)}
}

@Composable
private fun ProfileDialog(p: UserProfile, dismiss: () -> Unit, save: (UserProfile) -> Unit) {
    val resolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(p.displayName) }
    var username by remember { mutableStateOf(p.username) }
    var bio by remember { mutableStateOf(p.bio) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var photoPreview by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) selectedUri = uri
    }

    LaunchedEffect(p.uid, p.profileImageUrl) {
        photoPreview = p.profileImageUrl.takeIf { it.isNotBlank() }?.let {
            runCatching { ServiceLocator.storageRepository.getSignedMediaUrl(it) }.getOrNull()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) dismiss() },
        title = { Text("Hesap ve üyelik işlemleri") },
        text = {
            Column {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AsyncImage(
                        model = selectedUri ?: photoPreview,
                        contentDescription = "Profil fotoğrafı",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    IconButton(
                        onClick = { picker.launch("image/*") },
                        enabled = !saving,
                        modifier = Modifier.offset(x = 44.dp, y = 38.dp)
                    ) { Icon(Icons.Default.AddAPhoto, "Galeriden seç") }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Hesap ismi") }, enabled = !saving)
                OutlinedTextField(
                    username,
                    { username = it.lowercase(Locale.ROOT).filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '.' || ch == '_' }.take(20) },
                    label = { Text("Kullanıcı adı") }, enabled = !saving
                )
                OutlinedTextField(bio, { bio = it.take(160) }, label = { Text("Hakkımda") }, enabled = !saving, minLines = 3)
                if (selectedUri != null) Text("Yeni fotoğraf seçildi. Kaydettiğinde mevcut R2 sistemi kullanılacak.", color = MaterialTheme.colorScheme.primary)
                status?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                TextButton(
                    onClick = {
                        scope.launch {
                            runCatching {
                                com.google.firebase.auth.FirebaseAuth.getInstance().sendPasswordResetEmail(p.email).await()
                            }.onSuccess { status = "Şifre yenileme bağlantısı gönderildi." }
                             .onFailure { status = "Şifre yenileme bağlantısı gönderilemedi." }
                        }
                    },
                    enabled = !saving
                ) { Text("Şifre değiştirme bağlantısı gönder") }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.trim().length >= 2 && username.matches(Regex("[a-z0-9._]{3,20}")),
                onClick = {
                    scope.launch {
                        saving = true
                        status = null
                        try {
                            var photo = p.profileImageUrl
                            selectedUri?.let { uri ->
                                val bytes = withContext(Dispatchers.IO) {
                                    resolver.openInputStream(uri)?.use { it.readBytes() }
                                } ?: error("Profil fotoğrafı okunamadı.")
                                if (bytes.size > 8 * 1024 * 1024) error("Profil fotoğrafı 8 MB'dan büyük olamaz.")
                                val mime = resolver.getType(uri) ?: "image/jpeg"
                                val ext = when (mime.lowercase(Locale.ROOT)) {
                                    "image/png" -> "png"
                                    "image/webp" -> "webp"
                                    "image/heic" -> "heic"
                                    "image/heif" -> "heif"
                                    else -> "jpg"
                                }
                                when (val result = ServiceLocator.storageRepository.uploadMedia(StorageUploadRequest(fileName = "profile.$ext", bytes = bytes, contentType = mime, targetDirectory = "users/${p.uid}")).first()) {
                                    is AppResult.Success -> photo = result.data
                                    is AppResult.Error -> error(result.error.message)
                                }
                            }
                            save(p.copy(displayName = name.trim(), username = username.trim(), bio = bio.trim(), profileImageUrl = photo))
                            dismiss()
                        } catch (e: Exception) {
                            status = e.localizedMessage ?: "Profil güncellenemedi."
                        } finally {
                            saving = false
                        }
                    }
                }
            ) { Text(if (saving) "Kaydediliyor..." else "Kaydet") }
        },
        dismissButton = { TextButton(onClick = dismiss, enabled = !saving) { Text("İptal") } }
    )
}

@Composable
private fun AdminRolePage(uid: String, vm: AdminViewModel, dismiss: () -> Unit) {
    var role by remember { mutableStateOf<AdminRole?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var perms by remember { mutableStateOf(AdminPermissionSet()) }
    LaunchedEffect(uid) { role = vm.loadAdminRole(uid); role?.let { name = it.name; description = it.description; perms = it.permissions } }
    val presets = listOf(
        "Moderatör" to AdminPermissionSet(manageMembers = true, manageBans = true, managePosts = true, manageStories = true, manageGlobalChat = true, sendFeedback = true),
        "Sunucu Moderatörü" to AdminPermissionSet(manageMembers = true, manageBans = true, manageServers = true, manageGlobalChat = true),
        "İçerik Editörü" to AdminPermissionSet(managePosts = true, manageStories = true, manageBooks = true),
        "Yönetici" to AdminPermissionSet(manageMembers = true, manageBans = true, editProfiles = true, managePosts = true, manageStories = true, manageServers = true, manageGlobalChat = true, sendFeedback = true),
        "CEO" to AdminPermissionSet(manageMembers = true, manageBans = true, editProfiles = true, manageBooks = true, managePosts = true, manageStories = true, manageServers = true, manageGlobalChat = true, manageAdminRoles = true, manageCosmetics = true, sendFeedback = true, manageUpdates = true)
    )
    val permissionItems = listOf(
        "Üye yönetimi" to Triple("Üyeleri görüntüleme ve üye yönetim ekranlarını kullanma.", perms.manageMembers, { v: Boolean -> perms = perms.copy(manageMembers = v) }),
        "Ban / erişim kısıtlamaları" to Triple("Ban, süreli ban ve içerik/mesaj erişim kısıtlarını değiştirme.", perms.manageBans, { v: Boolean -> perms = perms.copy(manageBans = v) }),
        "Profil düzenleme" to Triple("Ad, kullanıcı adı, hakkımda ve profil fotoğrafını yönetme.", perms.editProfiles, { v: Boolean -> perms = perms.copy(editProfiles = v) }),
        "Kitap yönetimi" to Triple("Kitap ve yayın yönetim araçlarına erişme.", perms.manageBooks, { v: Boolean -> perms = perms.copy(manageBooks = v) }),
        "Gönderi yönetimi" to Triple("Gönderi moderasyonu ve yönetim işlemleri.", perms.managePosts, { v: Boolean -> perms = perms.copy(managePosts = v) }),
        "Hikâye yönetimi" to Triple("Hikâye moderasyonu ve yönetim işlemleri.", perms.manageStories, { v: Boolean -> perms = perms.copy(manageStories = v) }),
        "Sunucu yönetimi" to Triple("Sunucu oluşturma ve sunucu yönetim araçları.", perms.manageServers, { v: Boolean -> perms = perms.copy(manageServers = v) }),
        "Genel chat yönetimi" to Triple("Genel chat moderasyonu ve mesaj yönetimi.", perms.manageGlobalChat, { v: Boolean -> perms = perms.copy(manageGlobalChat = v) }),
        "Yönetici rolü verme" to Triple("Yönetici rolleri verme, düzenleme ve kaldırma.", perms.manageAdminRoles, { v: Boolean -> perms = perms.copy(manageAdminRoles = v) }),
        "Kozmetik rol yönetimi" to Triple("Kozmetik rozet oluşturma ve kullanıcıya atama.", perms.manageCosmetics, { v: Boolean -> perms = perms.copy(manageCosmetics = v) }),
        "Geri bildirim / DM" to Triple("Yönetim bildirimi ve doğrudan mesaj gönderme.", perms.sendFeedback, { v: Boolean -> perms = perms.copy(sendFeedback = v) }),
        "Uygulama güncellemeleri" to Triple("Yeni APK yayınlama ve aktif sürüm yönetimi.", perms.manageUpdates, { v: Boolean -> perms = perms.copy(manageUpdates = v) })
    )
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = dismiss) { Icon(Icons.Default.ArrowBack, "Geri") }
            Column { Text("Yönetici rolü", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)); Text(if (role == null) "Yeni yönetici rolü oluştur" else "Mevcut rolü düzenle", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text("Hazır roller", fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(presets.size) { index -> val preset = presets[index]; FilterChip(name == preset.first, { name = preset.first; perms = preset.second }, label = { Text(preset.first) }) } }
            }
            item { OutlinedTextField(name, { name = it.take(40) }, Modifier.fillMaxWidth(), label = { Text("Rol adı") }, singleLine = true); Spacer(Modifier.height(8.dp)); OutlinedTextField(description, { description = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Açıklama") }, minLines = 2) }
            item { Text("Yetkiler", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
            items(permissionItems.size) { index ->
                val item = permissionItems[index]
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(item.first, fontWeight = FontWeight.SemiBold); Text(item.second.first, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                        Switch(item.second.second, item.second.third)
                    }
                }
            }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { if (role != null) OutlinedButton(onClick = { vm.saveAdminRole(uid, null); dismiss() }, Modifier.weight(1f)) { Text("Rolü kaldır") }; Button(onClick = { vm.saveAdminRole(uid, AdminRole(id = role?.id ?: "", name = name.trim(), description = description.trim(), permissions = perms)); dismiss() }, enabled = name.trim().isNotBlank(), modifier = Modifier.weight(1f)) { Text("Kaydet") } } }
        }
    }
}
@Composable private fun CosmeticPage(p: UserProfile, vm: AdminViewModel, dismiss: () -> Unit) {
    val roles = vm.cosmeticRoles.collectAsState().value
    var assignedIds by remember(p.uid) { mutableStateOf(p.cosmeticRoleIds.toSet()) }
    var creating by remember { mutableStateOf(false) }
    if (creating) { CosmeticCreatePage(vm, { creating = false }); return }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = dismiss) { Icon(Icons.Default.ArrowBack, "Geri") }; Text("Kozmetik roller", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) }
        Spacer(Modifier.height(12.dp))
        Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) { Text("Yeni kozmetik rol / rozet oluştur") }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(roles, key = { it.id }) { role ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (role.imageUrl.isNotBlank()) {
                        var roleImage by remember(role.id) { mutableStateOf<String?>(null) }
                        LaunchedEffect(role.id, role.imageUrl) { roleImage = ServiceLocator.storageRepository.getSignedMediaUrl(role.imageUrl) }
                        AsyncImage(model = roleImage, contentDescription = role.name, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    } else Text(role.icon, modifier = Modifier.size(44.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(role.name, fontWeight = FontWeight.SemiBold); if (role.description.isNotBlank()) Text(role.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                    Switch(checked = role.id in assignedIds, onCheckedChange = { checked -> assignedIds = if (checked) assignedIds + role.id else assignedIds - role.id; vm.assignCosmetic(p.uid, role.id, checked) })
                }
            }
        } }
    }
}

@Composable private fun CosmeticCreatePage(vm: AdminViewModel, onBack: () -> Unit) {
    val resolver = androidx.compose.ui.platform.LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }; var icon by remember { mutableStateOf("✦") }; var description by remember { mutableStateOf("") }; var selectedImage by remember { mutableStateOf<android.net.Uri?>(null) }; var busy by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) selectedImage = it }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.Default.ArrowBack, "Geri") }; Text("Yeni kozmetik rol", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)) }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) { if (selectedImage != null) AsyncImage(model = selectedImage, contentDescription = "Rol görseli", modifier = Modifier.size(110.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Text(icon, style = MaterialTheme.typography.displaySmall) }
        Button(onClick = { picker.launch("image/*") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Galeriden rol görseli seç") }
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Rol adı") }, enabled = !busy)
        OutlinedTextField(icon, { icon = it.take(2) }, Modifier.fillMaxWidth(), label = { Text("Emoji / yedek simge") }, enabled = !busy)
        OutlinedTextField(description, { description = it.take(160) }, Modifier.fillMaxWidth(), label = { Text("Açıklama") }, enabled = !busy, minLines = 3)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                busy = true
                try {
                    var imageUrl = ""
                    selectedImage?.let { uri ->
                        val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } } ?: error("Görsel okunamadı.")
                        if (bytes.size > 8 * 1024 * 1024) error("Görsel 8 MBdan büyük olamaz.")
                        val mime = resolver.getType(uri) ?: "image/png"
                        val ext = mime.substringAfter("/").ifBlank { "png" }
                        when (val result = ServiceLocator.storageRepository.uploadMedia(StorageUploadRequest("role-" + System.currentTimeMillis() + "." + ext, bytes, mime, "admin/roles")).first()) {
                            is AppResult.Success -> imageUrl = result.data
                            is AppResult.Error -> error(result.error.message)
                        }
                    }
                    vm.saveCosmetic(CosmeticRole(name = name.trim(), icon = icon, description = description.trim(), imageUrl = imageUrl))
                    onBack()
                } catch (e: Exception) {
                    error = e.localizedMessage ?: "Rol oluşturulamadı."
                } finally {
                    busy = false
                }
            }
        }, enabled = !busy && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text(if (busy) "Yükleniyor..." else "Rolü oluştur")
        }
    }
}
@Composable private fun ContactDialog(p:UserProfile,dismiss:()->Unit){
    val scope=rememberCoroutineScope()
    var text by remember{mutableStateOf("")}
    var status by remember{mutableStateOf("")}
    val me=com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    AlertDialog(onDismissRequest=dismiss,title={Text("Kullanıcıya geri bildirim / DM")},text={Column{
        OutlinedTextField(text,{text=it},Modifier.fillMaxWidth(),label={Text("Mesaj")})
        Text(status)
    }},confirmButton={Row{
        TextButton(enabled=text.isNotBlank(),onClick={scope.launch{
            val r=ServiceLocator.notificationRepository.create(AppNotification(recipientId=p.uid,actorId=me,actorName="Libra Yönetim",type="ADMIN_FEEDBACK",title="Yönetimden bildirim",body=text.trim(),createdAt=System.currentTimeMillis()))
            status=if(r is AppResult.Success)"Bildirim gönderildi." else "Bildirim gönderilemedi."
        }}) { Text("Bildirim gönder") }
        TextButton(enabled=text.isNotBlank(),onClick={scope.launch{
            val r=ServiceLocator.chatRepository.sendDirectMessage(p.uid,text.trim())
            status=if(r is AppResult.Success)"DM gönderildi." else "DM gönderilemedi."
        }}) { Text("DM gönder") }
    }},dismissButton={TextButton(dismiss){Text("Kapat")}})
}