package com.libra.app.feature.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.libra.app.domain.model.AdminAudienceCategory
import com.libra.app.domain.model.AdminNotificationCampaignLog
import com.libra.app.domain.model.AdminNotificationDraft
import com.libra.app.domain.model.Book
import com.libra.app.domain.model.BookCategory
import com.libra.app.domain.model.CosmeticRole
import com.libra.app.domain.model.Post
import com.libra.app.domain.model.Story
import com.libra.app.domain.model.UserProfile
import com.libra.app.ui.animation.appleBouncy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DEFAULT_DRAFT_TAGS = listOf(
    "Duyuru",
    "Etkinlik",
    "Yeni Bölüm",
    "Sistem",
    "Topluluk",
    "Hatırlatma"
)

@Composable
fun AdminNotificationsScreen(
    vm: AdminViewModel,
    currentUid: String,
    currentDisplayName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val members by vm.members.collectAsState()
    val books by vm.books.collectAsState()
    val posts by vm.posts.collectAsState()
    val stories by vm.stories.collectAsState()
    val cosmeticRoles by vm.cosmeticRoles.collectAsState()
    val drafts by vm.notificationDrafts.collectAsState()
    val logs by vm.notificationLogs.collectAsState()
    val adminRoleUserIds by vm.adminRoleUserIds.collectAsState()
    val busy by vm.notificationActionBusy.collectAsState()
    val actionMessage by vm.notificationActionMessage.collectAsState()
    val errorMessage by vm.error.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Composer tab state
    var editingDraft by remember { mutableStateOf<AdminNotificationDraft?>(null) }
    var composeTitle by remember { mutableStateOf("") }
    var composeBody by remember { mutableStateOf("") }
    var composeTag by remember { mutableStateOf("Duyuru") }
    var composeAudience by remember { mutableStateOf(AdminAudienceCategory.ALL) }
    var composeSubId by remember { mutableStateOf("") }
    var composeCustomUserIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCustomUserPicker by remember { mutableStateOf(false) }
    var showRecipientsPreview by remember { mutableStateOf(false) }
    var showSendConfirm by remember { mutableStateOf(false) }

    // Drafts tab state
    var draftQuery by remember { mutableStateOf("") }
    var draftTagFilter by remember { mutableStateOf("Tümü") }
    var dialogEditingDraft by remember { mutableStateOf<AdminNotificationDraft?>(null) }
    var draftToConfirmSend by remember { mutableStateOf<AdminNotificationDraft?>(null) }
    var draftSendToEveryone by remember { mutableStateOf(false) }
    var draftToDelete by remember { mutableStateOf<AdminNotificationDraft?>(null) }

    val composerRecipients = remember(
        members,
        books,
        posts,
        stories,
        adminRoleUserIds,
        composeAudience,
        composeSubId,
        composeCustomUserIds
    ) {
        resolveAdminNotificationRecipients(
            members = members,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminRoleUserIds,
            categoryType = composeAudience,
            categorySubId = composeSubId,
            customUserIds = composeCustomUserIds
        )
    }

    val allEligibleRecipients = remember(members, books, posts, stories, adminRoleUserIds) {
        resolveAdminNotificationRecipients(
            members = members,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminRoleUserIds,
            categoryType = AdminAudienceCategory.ALL
        )
    }

    val composerAudienceLabel = remember(
        composeAudience,
        composeSubId,
        cosmeticRoles,
        composerRecipients.size
    ) {
        resolveAdminAudienceLabel(
            categoryType = composeAudience,
            categorySubId = composeSubId,
            cosmeticRoles = cosmeticRoles,
            customCount = composerRecipients.size
        )
    }

    fun resetComposer() {
        editingDraft = null
        composeTitle = ""
        composeBody = ""
        composeTag = "Duyuru"
        composeAudience = AdminAudienceCategory.ALL
        composeSubId = ""
        composeCustomUserIds = emptyList()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.appleBouncy(pressedScale = 0.86f, onClick = onBack)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bildirim Merkezi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${allEligibleRecipients.size} aktif üye • ${drafts.size} taslak • ${logs.size} gönderim",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val newDraftShape = RoundedCornerShape(14.dp)
                Surface(
                    modifier = Modifier
                        .clip(newDraftShape)
                        .appleBouncy(pressedScale = 0.94f, onClick = {
                            dialogEditingDraft = AdminNotificationDraft(
                                createdBy = currentUid,
                                tagLabel = "Duyuru",
                                categoryType = AdminAudienceCategory.ALL.key
                            )
                        }),
                    shape = newDraftShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Taslak",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Status / error banners
            actionMessage?.let { msg ->
                FeedbackBannerCard(
                    message = msg,
                    isError = false,
                    onDismiss = { vm.clearNotificationActionMessage() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            errorMessage?.let { err ->
                FeedbackBannerCard(
                    message = err,
                    isError = true,
                    onDismiss = { vm.clearError() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Segmented Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationHubTabButton(
                    title = "Gönder",
                    icon = Icons.AutoMirrored.Filled.Send,
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                NotificationHubTabButton(
                    title = "Taslaklar (${drafts.size})",
                    icon = Icons.Default.Bookmark,
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
                NotificationHubTabButton(
                    title = "Geçmiş (${logs.size})",
                    icon = Icons.Default.History,
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(4.dp))

            when (selectedTab) {
                0 -> ComposerTabContent(
                    editingDraft = editingDraft,
                    title = composeTitle,
                    body = composeBody,
                    tag = composeTag,
                    audience = composeAudience,
                    categorySubId = composeSubId,
                    customUserIds = composeCustomUserIds,
                    members = members,
                    books = books,
                    cosmeticRoles = cosmeticRoles,
                    recipients = composerRecipients,
                    audienceLabel = composerAudienceLabel,
                    busy = busy,
                    onTitleChange = { composeTitle = it },
                    onBodyChange = { composeBody = it },
                    onTagChange = { composeTag = it },
                    onAudienceChange = { next ->
                        composeAudience = next
                        composeSubId = when (next) {
                            AdminAudienceCategory.COSMETIC_ROLE -> cosmeticRoles.firstOrNull()?.id.orEmpty()
                            AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> BookCategory.FICTION.name
                            else -> ""
                        }
                    },
                    onSubIdChange = { composeSubId = it },
                    onOpenCustomPicker = { showCustomUserPicker = true },
                    onOpenRecipientsPreview = { showRecipientsPreview = true },
                    onClearEditing = { resetComposer() },
                    onSaveDraft = {
                        val draft = (editingDraft ?: AdminNotificationDraft()).copy(
                            title = composeTitle,
                            body = composeBody,
                            tagLabel = composeTag,
                            categoryType = composeAudience.key,
                            categorySubId = composeSubId,
                            customUserIds = composeCustomUserIds,
                            createdBy = (editingDraft?.createdBy?.ifBlank { currentUid }) ?: currentUid
                        )
                        vm.saveNotificationDraft(draft) { saved ->
                            if (saved != null) {
                                editingDraft = saved
                                selectedTab = 1
                            }
                        }
                    },
                    onSendNowClick = { showSendConfirm = true }
                )

                1 -> DraftsTabContent(
                    drafts = drafts,
                    query = draftQuery,
                    tagFilter = draftTagFilter,
                    members = members,
                    books = books,
                    posts = posts,
                    stories = stories,
                    cosmeticRoles = cosmeticRoles,
                    adminRoleUserIds = adminRoleUserIds,
                    busy = busy,
                    onQueryChange = { draftQuery = it },
                    onTagFilterChange = { draftTagFilter = it },
                    onCreateDraft = {
                        dialogEditingDraft = AdminNotificationDraft(
                            createdBy = currentUid,
                            tagLabel = "Duyuru",
                            categoryType = AdminAudienceCategory.ALL.key
                        )
                    },
                    onSendDraft = { draft ->
                        draftSendToEveryone = false
                        draftToConfirmSend = draft
                    },
                    onEditDraft = { draft -> dialogEditingDraft = draft },
                    onLoadIntoComposer = { draft ->
                        editingDraft = draft
                        composeTitle = draft.title
                        composeBody = draft.body
                        composeTag = draft.tagLabel.ifBlank { "Duyuru" }
                        composeAudience = AdminAudienceCategory.fromKey(draft.categoryType)
                        composeSubId = draft.categorySubId
                        composeCustomUserIds = draft.customUserIds
                        selectedTab = 0
                    },
                    onDeleteDraft = { draft -> draftToDelete = draft }
                )

                else -> CampaignHistoryTabContent(
                    logs = logs,
                    busy = busy,
                    onResendLog = { log ->
                        editingDraft = null
                        composeTitle = log.title
                        composeBody = log.body
                        composeTag = log.tagLabel.ifBlank { "Duyuru" }
                        composeAudience = AdminAudienceCategory.fromKey(log.categoryType)
                        composeSubId = when (composeAudience) {
                            AdminAudienceCategory.COSMETIC_ROLE -> cosmeticRoles.firstOrNull()?.id.orEmpty()
                            AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> BookCategory.FICTION.name
                            else -> ""
                        }
                        composeCustomUserIds = emptyList()
                        selectedTab = 0
                    },
                    onSaveAsDraft = { log ->
                        vm.saveNotificationDraft(
                            AdminNotificationDraft(
                                title = log.title,
                                body = log.body,
                                tagLabel = log.tagLabel.ifBlank { "Duyuru" },
                                categoryType = log.categoryType,
                                createdBy = currentUid
                            )
                        ) { saved ->
                            if (saved != null) selectedTab = 1
                        }
                    }
                )
            }
        }
    }

    if (showCustomUserPicker) {
        NotificationUserPickerDialog(
            members = allEligibleRecipients,
            selectedIds = composeCustomUserIds.toSet(),
            onDismiss = { showCustomUserPicker = false },
            onConfirm = { picked ->
                composeCustomUserIds = picked.toList()
                showCustomUserPicker = false
            }
        )
    }

    if (showRecipientsPreview) {
        RecipientsPreviewDialog(
            audienceLabel = composerAudienceLabel,
            recipients = composerRecipients,
            onDismiss = { showRecipientsPreview = false }
        )
    }

    if (showSendConfirm) {
        AlertDialog(
            onDismissRequest = { if (!busy) showSendConfirm = false },
            title = { Text("Bildirimi Gönder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "\"${composeTitle.trim()}\" başlıklı bildirim ${composerRecipients.size} üyeye gönderilecek.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Hedef: $composerAudienceLabel • Etiket: ${composeTag.ifBlank { "Duyuru" }}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = composeBody.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(
                        text = "Bildirim hem uygulama içi bildirim kutusuna hem de cihaz bildirim çubuğuna iletilir.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        vm.sendAdminNotificationBroadcast(
                            title = composeTitle,
                            body = composeBody,
                            categoryType = composeAudience,
                            categorySubId = composeSubId,
                            customUserIds = composeCustomUserIds,
                            tagLabel = composeTag,
                            senderUid = currentUid,
                            senderName = currentDisplayName.ifBlank { "Libra Yönetim" },
                            draftId = editingDraft?.id.orEmpty()
                        ) { ok, _ ->
                            if (ok) {
                                showSendConfirm = false
                                if (editingDraft == null) {
                                    composeTitle = ""
                                    composeBody = ""
                                }
                            }
                        }
                    }
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Evet, Gönder")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { showSendConfirm = false }) {
                    Text("İptal")
                }
            }
        )
    }

    dialogEditingDraft?.let { targetDraft ->
        DraftEditorDialog(
            initialDraft = targetDraft,
            members = members,
            allEligibleMembers = allEligibleRecipients,
            books = books,
            posts = posts,
            stories = stories,
            cosmeticRoles = cosmeticRoles,
            adminRoleUserIds = adminRoleUserIds,
            busy = busy,
            onDismiss = { if (!busy) dialogEditingDraft = null },
            onSave = { updated ->
                vm.saveNotificationDraft(updated) { saved ->
                    if (saved != null) {
                        dialogEditingDraft = null
                        selectedTab = 1
                    }
                }
            }
        )
    }

    draftToConfirmSend?.let { targetDraft ->
        val savedAudience = AdminAudienceCategory.fromKey(targetDraft.categoryType)
        val effectiveAudience = if (draftSendToEveryone) AdminAudienceCategory.ALL else savedAudience
        val effectiveRecipients = remember(
            targetDraft,
            draftSendToEveryone,
            members,
            books,
            posts,
            stories,
            adminRoleUserIds
        ) {
            resolveAdminNotificationRecipients(
                members = members,
                books = books,
                posts = posts,
                stories = stories,
                adminRoleUserIds = adminRoleUserIds,
                categoryType = effectiveAudience,
                categorySubId = if (draftSendToEveryone) "" else targetDraft.categorySubId,
                customUserIds = if (draftSendToEveryone) emptyList() else targetDraft.customUserIds
            )
        }
        val effectiveAudienceLabel = resolveAdminAudienceLabel(
            categoryType = effectiveAudience,
            categorySubId = if (draftSendToEveryone) "" else targetDraft.categorySubId,
            cosmeticRoles = cosmeticRoles,
            customCount = effectiveRecipients.size
        )

        AlertDialog(
            onDismissRequest = { if (!busy) draftToConfirmSend = null },
            title = { Text("Taslağı Gönder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = targetDraft.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = targetDraft.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Hedef: $effectiveAudienceLabel • ${effectiveRecipients.size} kişi",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (savedAudience != AdminAudienceCategory.ALL) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { draftSendToEveryone = !draftSendToEveryone }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Bu sefer herkese gönder",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Tüm üyelere (${allEligibleRecipients.size} kişi) yollamak için aç",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = draftSendToEveryone,
                                    onCheckedChange = { draftSendToEveryone = it }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !busy && effectiveRecipients.isNotEmpty(),
                    onClick = {
                        vm.sendAdminDraftNotification(
                            draft = targetDraft,
                            senderUid = currentUid,
                            senderName = currentDisplayName.ifBlank { "Libra Yönetim" },
                            sendToEveryone = draftSendToEveryone
                        ) { ok, _ ->
                            if (ok) draftToConfirmSend = null
                        }
                    }
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Gönder (${effectiveRecipients.size})")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { draftToConfirmSend = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    draftToDelete?.let { targetDraft ->
        AlertDialog(
            onDismissRequest = { if (!busy) draftToDelete = null },
            title = { Text("Taslağı Sil") },
            text = {
                Text("\"${targetDraft.title}\" taslağını kalıcı olarak silmek istiyor musun?")
            },
            confirmButton = {
                Button(
                    enabled = !busy,
                    onClick = {
                        vm.deleteNotificationDraft(targetDraft.id) { ok ->
                            if (ok) draftToDelete = null
                        }
                    }
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { draftToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComposerTabContent(
    editingDraft: AdminNotificationDraft?,
    title: String,
    body: String,
    tag: String,
    audience: AdminAudienceCategory,
    categorySubId: String,
    customUserIds: List<String>,
    members: List<UserProfile>,
    books: List<Book>,
    cosmeticRoles: List<CosmeticRole>,
    recipients: List<UserProfile>,
    audienceLabel: String,
    busy: Boolean,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onTagChange: (String) -> Unit,
    onAudienceChange: (AdminAudienceCategory) -> Unit,
    onSubIdChange: (String) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onOpenRecipientsPreview: () -> Unit,
    onClearEditing: () -> Unit,
    onSaveDraft: () -> Unit,
    onSendNowClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (editingDraft != null) {
            item {
                val shape = RoundedCornerShape(16.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Taslak Düzenleniyor",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = editingDraft.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(onClick = onClearEditing) {
                            Text("Sıfırla")
                        }
                    }
                }
            }
        }

        item {
            val cardShape = RoundedCornerShape(18.dp)
            Surface(
                shape = cardShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bildirim İçeriği",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DEFAULT_DRAFT_TAGS) { presetTag ->
                            FilterChip(
                                selected = tag.equals(presetTag, ignoreCase = true),
                                onClick = { onTagChange(presetTag) },
                                label = { Text(presetTag) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tag,
                        onValueChange = { if (it.length <= 24) onTagChange(it) },
                        label = { Text("Bildirim Kategorisi / Etiketi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { if (it.length <= 100) onTitleChange(it) },
                        label = { Text("Bildirim Başlığı") },
                        placeholder = { Text("Örn: Yeni Haftalık Etkinlik Başladı") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = body,
                        onValueChange = { if (it.length <= 500) onBodyChange(it) },
                        label = { Text("Bildirim Mesajı") },
                        placeholder = { Text("Üyelerin bildirim çubuğunda ve merkezinde göreceği metin...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            AudienceSelectorCard(
                audience = audience,
                categorySubId = categorySubId,
                customUserIds = customUserIds,
                members = members,
                books = books,
                cosmeticRoles = cosmeticRoles,
                onAudienceChange = onAudienceChange,
                onSubIdChange = onSubIdChange,
                onOpenCustomPicker = onOpenCustomPicker
            )
        }

        item {
            val previewShape = RoundedCornerShape(18.dp)
            Surface(
                shape = previewShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${recipients.size} üyeye gönderilecek",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = audienceLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        enabled = recipients.isNotEmpty(),
                        onClick = onOpenRecipientsPreview
                    ) {
                        Text("Alıcıları Gör")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    enabled = !busy && title.isNotBlank() && body.isNotBlank(),
                    onClick = onSaveDraft,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (editingDraft != null) "Taslağı Güncelle" else "Taslağa Kaydet")
                }

                Button(
                    enabled = !busy && title.isNotBlank() && body.isNotBlank() && recipients.isNotEmpty(),
                    onClick = onSendNowClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Gönder (${recipients.size})")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudienceSelectorCard(
    audience: AdminAudienceCategory,
    categorySubId: String,
    customUserIds: List<String>,
    members: List<UserProfile>,
    books: List<Book>,
    cosmeticRoles: List<CosmeticRole>,
    onAudienceChange: (AdminAudienceCategory) -> Unit,
    onSubIdChange: (String) -> Unit,
    onOpenCustomPicker: () -> Unit
) {
    val cardShape = RoundedCornerShape(18.dp)
    Surface(
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hedef Kitle Kategorisi",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminAudienceCategory.entries.forEach { cat ->
                    val selected = audience == cat
                    val chipShape = RoundedCornerShape(12.dp)
                    Surface(
                        modifier = Modifier
                            .clip(chipShape)
                            .appleBouncy(pressedScale = 0.95f, onClick = { onAudienceChange(cat) }),
                        shape = chipShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = audienceCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = cat.shortLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }

            Text(
                text = audience.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (audience) {
                AdminAudienceCategory.COSMETIC_ROLE -> {
                    if (cosmeticRoles.isEmpty()) {
                        Text(
                            text = "Henüz kozmetik rozet tanımlanmamış.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            cosmeticRoles.forEach { role ->
                                val wearers = members.count {
                                    !it.moderation.isBanned && role.id in it.cosmeticRoleIds
                                }
                                FilterChip(
                                    selected = categorySubId == role.id,
                                    onClick = { onSubIdChange(role.id) },
                                    label = { Text("${role.icon} ${role.name} ($wearers)") }
                                )
                            }
                        }
                    }
                }

                AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BookCategory.entries.forEach { cat ->
                            val authorCount = books
                                .asSequence()
                                .filter { it.category == cat && it.ownerId.isNotBlank() }
                                .map { it.ownerId }
                                .toSet()
                                .size
                            FilterChip(
                                selected = categorySubId.equals(cat.name, ignoreCase = true),
                                onClick = { onSubIdChange(cat.name) },
                                label = { Text("${cat.displayName} ($authorCount)") }
                            )
                        }
                    }
                }

                AdminAudienceCategory.CUSTOM_USERS -> {
                    val memberLookup = remember(members) { members.associateBy { it.uid } }
                    OutlinedButton(
                        onClick = onOpenCustomPicker,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Üyelerden Seç (${customUserIds.size} kişi)")
                    }
                    if (customUserIds.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            customUserIds.take(12).forEach { uid ->
                                val user = memberLookup[uid]
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Text(
                                        text = user?.displayName?.ifBlank { user.username } ?: uid.take(8),
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            if (customUserIds.size > 12) {
                                Text(
                                    text = "+${customUserIds.size - 12} kişi daha",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }
}

@Composable
private fun DraftsTabContent(
    drafts: List<AdminNotificationDraft>,
    query: String,
    tagFilter: String,
    members: List<UserProfile>,
    books: List<Book>,
    posts: List<Post>,
    stories: List<Story>,
    cosmeticRoles: List<CosmeticRole>,
    adminRoleUserIds: Set<String>,
    busy: Boolean,
    onQueryChange: (String) -> Unit,
    onTagFilterChange: (String) -> Unit,
    onCreateDraft: () -> Unit,
    onSendDraft: (AdminNotificationDraft) -> Unit,
    onEditDraft: (AdminNotificationDraft) -> Unit,
    onLoadIntoComposer: (AdminNotificationDraft) -> Unit,
    onDeleteDraft: (AdminNotificationDraft) -> Unit
) {
    val tagOptions = remember(drafts) {
        val dynamic = drafts.mapNotNull { it.tagLabel.trim().takeIf(String::isNotBlank) }
        (listOf("Tümü") + DEFAULT_DRAFT_TAGS + dynamic).distinct()
    }
    val filteredDrafts = remember(drafts, query, tagFilter) {
        filterAndSortAdminNotificationDrafts(drafts, query, tagFilter)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text("Taslaklarda ara...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = if (query.isNotBlank()) {
                            {
                                IconButton(onClick = { onQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Temizle")
                                }
                            }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = onCreateDraft,
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Yeni")
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tagOptions) { tag ->
                        FilterChip(
                            selected = tagFilter.equals(tag, ignoreCase = true),
                            onClick = { onTagFilterChange(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        if (filteredDrafts.isEmpty()) {
            item {
                val emptyShape = RoundedCornerShape(18.dp)
                Surface(
                    shape = emptyShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = if (drafts.isEmpty()) "Henüz bildirim taslağı yok" else "Aramaya uygun taslak bulunamadı",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Hazır bildirim taslakları ekleyerek tek dokunuşla tüm üyelere veya belirli gruplara tekrar tekrar bildirim gönderebilirsin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = onCreateDraft, shape = RoundedCornerShape(14.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("İlk Taslağı Oluştur")
                        }
                    }
                }
            }
        } else {
            items(filteredDrafts, key = { it.id }) { draft ->
                val cat = AdminAudienceCategory.fromKey(draft.categoryType)
                val targetRecipients = remember(draft, members, books, posts, stories, adminRoleUserIds) {
                    resolveAdminNotificationRecipients(
                        members = members,
                        books = books,
                        posts = posts,
                        stories = stories,
                        adminRoleUserIds = adminRoleUserIds,
                        categoryType = cat,
                        categorySubId = draft.categorySubId,
                        customUserIds = draft.customUserIds
                    )
                }
                val audienceLabel = resolveAdminAudienceLabel(
                    categoryType = cat,
                    categorySubId = draft.categorySubId,
                    cosmeticRoles = cosmeticRoles,
                    customCount = targetRecipients.size
                )

                val cardShape = RoundedCornerShape(18.dp)
                Surface(
                    shape = cardShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillBadge(
                                text = draft.tagLabel.ifBlank { "Duyuru" },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PillBadge(
                                text = "$audienceLabel (${targetRecipients.size})",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            if (draft.sendCount > 0) {
                                PillBadge(
                                    text = "${draft.sendCount}x",
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = draft.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = draft.body,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (draft.lastSentAt > 0L) {
                            Text(
                                text = "Son gönderim: ${formatAdminNotificationDate(draft.lastSentAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                enabled = !busy,
                                onClick = { onSendDraft(draft) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Gönder")
                            }
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { onEditDraft(draft) },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("Düzenle")
                            }
                            TextButton(
                                enabled = !busy,
                                onClick = { onLoadIntoComposer(draft) }
                            ) {
                                Text("İçeriğe Al")
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(
                                enabled = !busy,
                                onClick = { onDeleteDraft(draft) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CampaignHistoryTabContent(
    logs: List<AdminNotificationCampaignLog>,
    busy: Boolean,
    onResendLog: (AdminNotificationCampaignLog) -> Unit,
    onSaveAsDraft: (AdminNotificationCampaignLog) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (logs.isEmpty()) {
            item {
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "Henüz gönderilmiş duyuru yok",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Gönderdiğin tüm toplu ve kategorize edilmiş bildirim kampanyaları burada kayıt altında tutulur.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(logs, key = { it.id.ifBlank { "${it.createdAt}_${it.title}" } }) { log ->
                val shape = RoundedCornerShape(18.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PillBadge(
                                text = log.tagLabel.ifBlank { "Duyuru" },
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            PillBadge(
                                text = log.categoryLabel.ifBlank { AdminAudienceCategory.fromKey(log.categoryType).label },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.weight(1f))
                            PillBadge(
                                text = "${log.recipientCount} alıcı",
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = log.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = log.body,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${log.senderName.ifBlank { "Libra Yönetim" }} • ${formatAdminNotificationDate(log.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = !busy,
                                onClick = { onResendLog(log) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Tekrar Hazırla")
                            }
                            TextButton(
                                enabled = !busy,
                                onClick = { onSaveAsDraft(log) }
                            ) {
                                Text("Taslağa Kaydet")
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun DraftEditorDialog(
    initialDraft: AdminNotificationDraft,
    members: List<UserProfile>,
    allEligibleMembers: List<UserProfile>,
    books: List<Book>,
    posts: List<Post>,
    stories: List<Story>,
    cosmeticRoles: List<CosmeticRole>,
    adminRoleUserIds: Set<String>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (AdminNotificationDraft) -> Unit
) {
    var title by remember(initialDraft) { mutableStateOf(initialDraft.title) }
    var body by remember(initialDraft) { mutableStateOf(initialDraft.body) }
    var tag by remember(initialDraft) { mutableStateOf(initialDraft.tagLabel.ifBlank { "Duyuru" }) }
    var audience by remember(initialDraft) {
        mutableStateOf(AdminAudienceCategory.fromKey(initialDraft.categoryType))
    }
    var subId by remember(initialDraft) { mutableStateOf(initialDraft.categorySubId) }
    var customUserIds by remember(initialDraft) { mutableStateOf(initialDraft.customUserIds) }
    var showPicker by remember { mutableStateOf(false) }

    val recipients = remember(
        members,
        books,
        posts,
        stories,
        adminRoleUserIds,
        audience,
        subId,
        customUserIds
    ) {
        resolveAdminNotificationRecipients(
            members = members,
            books = books,
            posts = posts,
            stories = stories,
            adminRoleUserIds = adminRoleUserIds,
            categoryType = audience,
            categorySubId = subId,
            customUserIds = customUserIds
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialDraft.id.isBlank()) "Yeni Bildirim Taslağı" else "Taslağı Düzenle")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(DEFAULT_DRAFT_TAGS) { preset ->
                        FilterChip(
                            selected = tag.equals(preset, ignoreCase = true),
                            onClick = { tag = preset },
                            label = { Text(preset) }
                        )
                    }
                }
                OutlinedTextField(
                    value = tag,
                    onValueChange = { if (it.length <= 24) tag = it },
                    label = { Text("Etiket") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Başlık") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { if (it.length <= 500) body = it },
                    label = { Text("Bildirim Metni") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                AudienceSelectorCard(
                    audience = audience,
                    categorySubId = subId,
                    customUserIds = customUserIds,
                    members = members,
                    books = books,
                    cosmeticRoles = cosmeticRoles,
                    onAudienceChange = { next ->
                        audience = next
                        subId = when (next) {
                            AdminAudienceCategory.COSMETIC_ROLE -> cosmeticRoles.firstOrNull()?.id.orEmpty()
                            AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> BookCategory.FICTION.name
                            else -> ""
                        }
                    },
                    onSubIdChange = { subId = it },
                    onOpenCustomPicker = { showPicker = true }
                )
                Text(
                    text = "Mevcut hedef alıcı sayısı: ${recipients.size} üye",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !busy && title.isNotBlank() && body.isNotBlank(),
                onClick = {
                    onSave(
                        initialDraft.copy(
                            title = title.trim(),
                            body = body.trim(),
                            tagLabel = tag.trim().ifBlank { "Duyuru" },
                            categoryType = audience.key,
                            categorySubId = subId,
                            customUserIds = customUserIds
                        )
                    )
                }
            ) {
                Text(if (initialDraft.id.isBlank()) "Kaydet" else "Güncelle")
            }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) {
                Text("İptal")
            }
        }
    )

    if (showPicker) {
        NotificationUserPickerDialog(
            members = allEligibleMembers,
            selectedIds = customUserIds.toSet(),
            onDismiss = { showPicker = false },
            onConfirm = { picked ->
                customUserIds = picked.toList()
                showPicker = false
            }
        )
    }
}

@Composable
private fun NotificationUserPickerDialog(
    members: List<UserProfile>,
    selectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (Set<String>) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var workingSelection by remember(selectedIds) { mutableStateOf(selectedIds) }

    val filtered = remember(members, query) {
        val clean = query.trim().lowercase()
        if (clean.isBlank()) members
        else members.filter {
            it.displayName.lowercase().contains(clean) ||
                it.username.lowercase().contains(clean) ||
                it.email.lowercase().contains(clean)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Alıcı Üyeleri Seç (${workingSelection.size})") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Ad veya @kullanıcı adı ara...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { workingSelection = filtered.map { it.uid }.toSet() }) {
                        Text("Filtrelenenleri Seç")
                    }
                    TextButton(onClick = { workingSelection = emptySet() }) {
                        Text("Temizle")
                    }
                }
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.uid }) { user ->
                        val checked = user.uid in workingSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    workingSelection = if (checked) {
                                        workingSelection - user.uid
                                    } else {
                                        workingSelection + user.uid
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = { nowChecked ->
                                    workingSelection = if (nowChecked) {
                                        workingSelection + user.uid
                                    } else {
                                        workingSelection - user.uid
                                    }
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            UserCompactAvatar(user = user)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName.ifBlank { user.username.ifBlank { "Kullanıcı" } },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.username.isNotBlank()) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(workingSelection) }) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Seç (${workingSelection.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}

@Composable
private fun RecipientsPreviewDialog(
    audienceLabel: String,
    recipients: List<UserProfile>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$audienceLabel (${recipients.size})") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recipients, key = { it.uid }) { user ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserCompactAvatar(user = user)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName.ifBlank { user.username.ifBlank { "Libra Üyesi" } },
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (user.username.isNotBlank()) "@${user.username}" else user.uid.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

@Composable
private fun NotificationHubTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .appleBouncy(pressedScale = 0.96f, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FeedbackBannerCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(
            1.dp,
            if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                tint = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PillBadge(
    text: String,
    containerColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun UserCompactAvatar(user: UserProfile) {
    if (user.profileImageUrl.isNotBlank()) {
        AsyncImage(
            model = user.profileImageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.initials.ifBlank { "L" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun audienceCategoryIcon(category: AdminAudienceCategory): ImageVector = when (category) {
    AdminAudienceCategory.ALL -> Icons.Default.Campaign
    AdminAudienceCategory.AUTHORS -> Icons.AutoMirrored.Filled.MenuBook
    AdminAudienceCategory.CREATORS -> Icons.Default.Edit
    AdminAudienceCategory.NEW_MEMBERS -> Icons.Default.PersonAdd
    AdminAudienceCategory.COSMETIC_ROLE -> Icons.Default.Star
    AdminAudienceCategory.BOOK_CATEGORY_AUTHORS -> Icons.AutoMirrored.Filled.MenuBook
    AdminAudienceCategory.ADMINS -> Icons.Default.AdminPanelSettings
    AdminAudienceCategory.CUSTOM_USERS -> Icons.Default.People
}

private fun formatAdminNotificationDate(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("tr", "TR")).format(Date(timestampMillis))
}
