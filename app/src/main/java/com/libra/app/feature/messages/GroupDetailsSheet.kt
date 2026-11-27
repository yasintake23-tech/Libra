package com.libra.app.feature.messages

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.libra.app.core.di.ServiceLocator
import com.libra.app.core.result.AppResult
import com.libra.app.domain.model.DirectConversation
import com.libra.app.domain.model.DirectMessage
import com.libra.app.domain.model.GroupMemberRole
import com.libra.app.domain.model.GroupPermissions
import com.libra.app.domain.model.StorageUploadRequest
import com.libra.app.domain.model.UserProfile
import com.libra.app.domain.model.canAddGroupMembers
import com.libra.app.domain.model.canEditGroupInfo
import com.libra.app.domain.model.canManageGroupSettings
import com.libra.app.domain.model.resolveGroupMemberRole
import com.libra.app.domain.model.resolveGroupOwnerId
import com.libra.app.ui.animation.appleBouncy
import com.libra.app.ui.components.FullScreenImageViewerDialog
import com.libra.app.ui.components.UserAvatar
import com.libra.app.ui.theme.LocalLibraDarkTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LibraLavender = Color(0xFF2554C7)
private val LibraLavenderSurface = Color(0xFFEAF0FE)
private val LibraLavenderDarkSurface = Color(0xFF19243F)
private val LibraLavenderBorder = Color(0xFFC7D8FC)
private val LibraLavenderDarkBorder = Color(0xFF2B3E6B)
private val LibraLavenderText = Color(0xFF1E44A6)
private val LibraLavenderDarkText = Color(0xFF9BB8FF)

private enum class GroupAboutTab(val label: String) {
    OVERVIEW("Genel"),
    MEMBERS("Üyeler"),
    MEDIA("Medya"),
    SETTINGS("Ayarlar")
}

private enum class GroupAboutPage { STUDIO, ADD_MEMBERS }

@Composable
fun GroupDetailsSheet(
    conversation: DirectConversation,
    currentUid: String,
    allConversations: List<DirectConversation>,
    messages: List<DirectMessage> = emptyList(),
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onUpdateInfo: (name: String, description: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onUpdateMedia: (photoUrl: String, bannerUrl: String, onDone: (Boolean, String?) -> Unit) -> Unit = { _, _, done -> done(false, null) },
    onUpdatePermissions: (GroupPermissions, onDone: (Boolean, String?) -> Unit) -> Unit,
    onAddMembers: (List<String>, onDone: (Boolean, String?) -> Unit) -> Unit,
    onRemoveMember: (targetUid: String, onDone: (Boolean, String?) -> Unit) -> Unit,
    onSetAdmin: (targetUid: String, isAdmin: Boolean, onDone: (Boolean, String?) -> Unit) -> Unit,
    onClearConversation: ((Boolean) -> Unit) -> Unit = { it(false) },
    onLeaveCompleted: () -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val presidentUid = remember(conversation) { resolveGroupOwnerId(conversation) }
    val isManager = remember(conversation, currentUid) { canManageGroupSettings(conversation, currentUid) }
    val canEditInfo = remember(conversation, currentUid) { canEditGroupInfo(conversation, currentUid) }
    val canAddMembers = remember(conversation, currentUid) { canAddGroupMembers(conversation, currentUid) }

    var page by remember { mutableStateOf(GroupAboutPage.STUDIO) }
    var activeTab by remember { mutableStateOf(GroupAboutTab.OVERVIEW) }

    var isEditingInfo by remember { mutableStateOf(false) }
    var nameDraft by remember(conversation.groupName) { mutableStateOf(conversation.groupName) }
    var descDraft by remember(conversation.groupDescription) { mutableStateOf(conversation.groupDescription) }
    var savingInfo by remember { mutableStateOf(false) }

    var uploadingAvatar by remember { mutableStateOf(false) }
    var uploadingBanner by remember { mutableStateOf(false) }

    var permissionsDraft by remember(conversation.groupPermissions) {
        mutableStateOf(conversation.groupPermissions)
    }
    var savingPerms by remember { mutableStateOf(false) }

    var memberSearch by remember { mutableStateOf("") }
    var confirmKickMember by remember { mutableStateOf<UserProfile?>(null) }
    var confirmLeaveGroup by remember { mutableStateOf(false) }
    var confirmClearChat by remember { mutableStateOf(false) }
    var actionBusy by remember { mutableStateOf(false) }
    var fullscreenMediaUrl by remember { mutableStateOf<String?>(null) }

    val memberProfiles = remember { mutableStateMapOf<String, UserProfile>() }
    var loadingProfiles by remember { mutableStateOf(false) }

    fun uploadAndSaveGroupImage(uri: Uri, isBanner: Boolean) {
        if (currentUid.isBlank() || !canEditInfo) return
        if (isBanner) uploadingBanner = true else uploadingAvatar = true
        scope.launch {
            try {
                val prepared = prepareChatImageForUpload(
                    context = context,
                    uri = uri,
                    prefix = if (isBanner) "grp_banner" else "grp_avatar"
                )
                val req = StorageUploadRequest(
                    fileName = prepared.fileName,
                    bytes = prepared.bytes,
                    contentType = prepared.contentType,
                    targetDirectory = "users/$currentUid"
                )
                when (val uploadRes = ServiceLocator.storageRepository.uploadMedia(req).first()) {
                    is AppResult.Success -> {
                        val nextPhoto = if (isBanner) conversation.groupPhotoUrl else uploadRes.data
                        val nextBanner = if (isBanner) uploadRes.data else conversation.groupBannerUrl
                        onUpdateMedia(nextPhoto, nextBanner) { ok, err ->
                            if (ok) {
                                Toast.makeText(
                                    context,
                                    if (isBanner) "Grup kapak görseli güncellendi." else "Grup profil fotoğrafı güncellendi.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else if (!err.isNullOrBlank()) {
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    is AppResult.Error -> {
                        Toast.makeText(context, uploadRes.error.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.localizedMessage ?: "Görsel yüklenemedi.", Toast.LENGTH_SHORT).show()
            } finally {
                if (isBanner) uploadingBanner = false else uploadingAvatar = false
            }
        }
    }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadAndSaveGroupImage(it, isBanner = false) }
    }
    val bannerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { uploadAndSaveGroupImage(it, isBanner = true) }
    }

    LaunchedEffect(conversation.participantIds) {
        val missing = conversation.participantIds.filter { it.isNotBlank() && !memberProfiles.containsKey(it) }
        if (missing.isEmpty()) return@LaunchedEffect
        loadingProfiles = true
        runCatching {
            coroutineScope {
                missing.map { uid ->
                    async {
                        val loaded = (ServiceLocator.userRepository.getUserProfileFresh(uid) as? AppResult.Success)?.data
                        uid to (loaded ?: UserProfile(uid = uid, displayName = "Libra Üyesi"))
                    }
                }.awaitAll()
            }
        }.onSuccess { pairs ->
            pairs.forEach { (uid, profile) -> memberProfiles[uid] = profile }
        }
        loadingProfiles = false
    }

    val sortedMemberUids = remember(conversation.participantIds, presidentUid, conversation.adminIds, memberProfiles.size) {
        val roleRank: (String) -> Int = { uid ->
            when (resolveGroupMemberRole(conversation, uid)) {
                GroupMemberRole.PRESIDENT -> 0
                GroupMemberRole.ADMIN -> 1
                GroupMemberRole.MEMBER -> 2
            }
        }
        conversation.participantIds.distinct().sortedWith(
            compareBy<String>({ roleRank(it) }, {
                memberProfiles[it]?.displayName?.lowercase(Locale.getDefault()).orEmpty()
            })
        )
    }

    val filteredMemberUids = remember(sortedMemberUids, memberSearch, memberProfiles.size) {
        val q = memberSearch.trim()
        if (q.isBlank()) sortedMemberUids
        else sortedMemberUids.filter { uid ->
            val p = memberProfiles[uid]
            uid.contains(q, ignoreCase = true) ||
                p?.displayName?.contains(q, ignoreCase = true) == true ||
                p?.username?.contains(q, ignoreCase = true) == true
        }
    }

    val mediaMessages = remember(messages) {
        messages.filter { it.mediaUrl.isNotBlank() }.sortedByDescending { it.createdAt }
    }
    val sharedItemsMessages = remember(messages) {
        messages.filter {
            it.sharedContent != null ||
                it.text.contains("http://", ignoreCase = true) ||
                it.text.contains("https://", ignoreCase = true)
        }.sortedByDescending { it.createdAt }
    }

    val presidentProfile = memberProfiles[presidentUid]
        ?: UserProfile(uid = presidentUid, displayName = "Grup Başkanı")
    val adminCount = remember(conversation.participantIds, conversation.adminIds, presidentUid) {
        conversation.participantIds.count { uid ->
            val r = resolveGroupMemberRole(conversation, uid)
            r == GroupMemberRole.PRESIDENT || r == GroupMemberRole.ADMIN
        }
    }

    fun shareGroupInvite() {
        val descPart = conversation.groupDescription.takeIf { it.isNotBlank() }?.let { "\n$it" }.orEmpty()
        val inviteText = "Libra'daki \"${conversation.groupName.ifBlank { "Grup Sohbeti" }}\" grubumuza katıl! " +
            "(${conversation.participantIds.size} katılımcı)$descPart\nLibra uygulamasından gruba eklenmek için bana mesaj at."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteText)
        }
        runCatching { context.startActivity(Intent.createChooser(intent, "Grup Davetini Paylaş")) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false
        )
    ) {
        BackHandler {
            if (page == GroupAboutPage.ADD_MEMBERS) {
                page = GroupAboutPage.STUDIO
            } else {
                onDismiss()
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = page,
                label = "group_about_page_switch"
            ) { currentPage ->
                when (currentPage) {
                    GroupAboutPage.ADD_MEMBERS -> {
                        AddGroupMembersFullPage(
                            conversation = conversation,
                            currentUid = currentUid,
                            allConversations = allConversations,
                            onBack = { page = GroupAboutPage.STUDIO },
                            onShareInvite = ::shareGroupInvite,
                            onConfirmAdd = { selectedIds, done ->
                                onAddMembers(selectedIds) { ok, err ->
                                    if (ok) {
                                        Toast.makeText(
                                            context,
                                            "${selectedIds.size} kişi gruba eklendi.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        done(true)
                                        page = GroupAboutPage.STUDIO
                                    } else {
                                        Toast.makeText(
                                            context,
                                            err ?: "Kişiler eklenemedi.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        done(false)
                                    }
                                }
                            }
                        )
                    }

                    GroupAboutPage.STUDIO -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .navigationBarsPadding()
                                .padding(bottom = 28.dp)
                        ) {
                            // Full-Bleed Architectural Cover + Floating Squircle Group Avatar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(216.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(172.dp)
                                ) {
                                    LibraArchitecturalBanner(
                                        seedKey = conversation.id.ifBlank { conversation.groupName },
                                        bannerMediaKey = conversation.groupBannerUrl,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Top readability scrim
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(92.dp)
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Black.copy(alpha = 0.56f), Color.Transparent)
                                                )
                                            )
                                    )
                                    // Back + Cover Edit Bar
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .appleBouncy(pressedScale = 0.88f, onClick = onDismiss),
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.45f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Geri",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.weight(1f))
                                        if (canEditInfo) {
                                            Surface(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .appleBouncy(
                                                        enabled = !uploadingBanner,
                                                        pressedScale = 0.92f,
                                                        onClick = {
                                                            bannerPicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                            )
                                                        }
                                                    ),
                                                shape = CircleShape,
                                                color = Color.Black.copy(alpha = 0.48f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    if (uploadingBanner) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(13.dp),
                                                            strokeWidth = 2.dp,
                                                            color = Color.White
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Image,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                    Text(
                                                        text = "Kapağı Düzenle",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Floating Squircle Group Avatar
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 18.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = MaterialTheme.colorScheme.background,
                                        border = BorderStroke(4.dp, MaterialTheme.colorScheme.background),
                                        shadowElevation = 6.dp
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            LibraSquircleAvatar(
                                                photoUrl = conversation.groupPhotoUrl,
                                                title = conversation.groupName.ifBlank { "Grup" },
                                                size = 84.dp,
                                                cornerRadius = 21.dp,
                                                onClick = if (canEditInfo && !uploadingAvatar) {
                                                    {
                                                        avatarPicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    }
                                                } else null
                                            )
                                            if (uploadingAvatar) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(84.dp)
                                                        .clip(RoundedCornerShape(21.dp))
                                                        .background(Color.Black.copy(alpha = 0.5f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(22.dp),
                                                        strokeWidth = 2.dp,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (canEditInfo) {
                                        Surface(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .appleBouncy(
                                                    enabled = !uploadingAvatar,
                                                    pressedScale = 0.88f,
                                                    onClick = {
                                                        avatarPicker.launch(
                                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                        )
                                                    }
                                                ),
                                            shape = CircleShape,
                                            color = LibraLavender,
                                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.background)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.AddAPhoto,
                                                    contentDescription = "Grup fotoğrafını değiştir",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Quick CTA row aligned bottom right
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 18.dp, bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (canAddMembers) {
                                        Surface(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .appleBouncy(pressedScale = 0.92f, onClick = { page = GroupAboutPage.ADD_MEMBERS }),
                                            shape = CircleShape,
                                            color = LibraLavender
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                Icon(Icons.Default.PersonAdd, null, tint = Color.White, modifier = Modifier.size(15.dp))
                                                Text(
                                                    "Kişi Ekle",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .appleBouncy(pressedScale = 0.88f, onClick = ::shareGroupInvite),
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Grubu paylaş",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Group Title & Editorial Identity Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = conversation.groupName.ifBlank { "Grup Sohbeti" },
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.4).sp
                                        ),
                                        modifier = Modifier.weight(1f, fill = false),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (canEditInfo && !isEditingInfo) {
                                        Surface(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .appleBouncy(pressedScale = 0.88f, onClick = { isEditingInfo = true }),
                                            shape = CircleShape,
                                            color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Düzenle",
                                                    tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (isEditingInfo) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, LibraLavender.copy(alpha = 0.5f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = nameDraft,
                                                onValueChange = { if (it.length <= 60) nameDraft = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                label = { Text("Grup Adı") },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            OutlinedTextField(
                                                value = descDraft,
                                                onValueChange = { if (it.length <= 300) descDraft = it },
                                                modifier = Modifier.fillMaxWidth(),
                                                minLines = 2,
                                                maxLines = 4,
                                                label = { Text("Grup Açıklaması") },
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                                            ) {
                                                TextButton(
                                                    enabled = !savingInfo,
                                                    onClick = {
                                                        nameDraft = conversation.groupName
                                                        descDraft = conversation.groupDescription
                                                        isEditingInfo = false
                                                    }
                                                ) { Text("Vazgeç") }
                                                Button(
                                                    enabled = !savingInfo && nameDraft.trim().isNotBlank(),
                                                    colors = ButtonDefaults.buttonColors(containerColor = LibraLavender),
                                                    shape = RoundedCornerShape(12.dp),
                                                    onClick = {
                                                        savingInfo = true
                                                        onUpdateInfo(nameDraft.trim(), descDraft.trim()) { ok, err ->
                                                            savingInfo = false
                                                            if (ok) {
                                                                isEditingInfo = false
                                                                Toast.makeText(context, "Grup bilgileri güncellendi.", Toast.LENGTH_SHORT).show()
                                                            } else if (!err.isNullOrBlank()) {
                                                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                ) {
                                                    if (savingInfo) {
                                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                                                    } else {
                                                        Text("Kaydet", color = Color.White, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = conversation.groupDescription.ifBlank {
                                            if (canEditInfo) "Grup açıklaması eklemek için düzenle simgesine dokun."
                                            else "Grup açıklaması henüz eklenmemiş."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // 3-Stat Architectural Strip
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    GroupMetricTile(
                                        value = sortedMemberUids.size.toString(),
                                        label = "Katılımcı",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GroupMetricTile(
                                        value = adminCount.toString(),
                                        label = "Yönetici",
                                        modifier = Modifier.weight(1f)
                                    )
                                    GroupMetricTile(
                                        value = (mediaMessages.size + sharedItemsMessages.size).toString(),
                                        label = "Medya",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // 4-Tab Studio Switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 18.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GroupAboutTab.entries.forEach { tab ->
                                    val selected = activeTab == tab
                                    val badge = when (tab) {
                                        GroupAboutTab.MEMBERS -> sortedMemberUids.size
                                        GroupAboutTab.MEDIA -> mediaMessages.size + sharedItemsMessages.size
                                        else -> 0
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .appleBouncy(pressedScale = 0.94f, onClick = { activeTab = tab }),
                                        shape = CircleShape,
                                        color = if (selected) LibraLavender else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) LibraLavender else MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = tab.label,
                                                style = MaterialTheme.typography.labelLarge.copy(
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 13.sp
                                                ),
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (badge > 0) {
                                                Text(
                                                    text = badge.toString(),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (selected) Color.White.copy(alpha = 0.9f)
                                                    else if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Tab Content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                when (activeTab) {
                                    GroupAboutTab.OVERVIEW -> {
                                        // Group President Spotlight
                                        if (presidentUid.isNotBlank()) {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text(
                                                    text = "Grup Başkanı",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(18.dp))
                                                        .clickable {
                                                            onDismiss()
                                                            onOpenProfile(presidentUid)
                                                        },
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = if (darkTheme) LibraLavenderDarkSurface.copy(alpha = 0.5f)
                                                    else LibraLavenderSurface.copy(alpha = 0.6f),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (darkTheme) LibraLavenderDarkBorder else LibraLavenderBorder
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        UserAvatar(
                                                            photoUrl = presidentProfile.profileImageUrl,
                                                            initials = presidentProfile.initials,
                                                            size = 46.dp
                                                        )
                                                        Column(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .padding(horizontal = 12.dp)
                                                        ) {
                                                            Text(
                                                                text = presidentProfile.displayName.ifBlank { "Libra Üyesi" } +
                                                                    if (presidentUid == currentUid) " (Sen)" else "",
                                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                            Text(
                                                                text = if (presidentProfile.username.isNotBlank()) {
                                                                    "@${presidentProfile.username} • Kurucu & Başkan"
                                                                } else {
                                                                    "Grup Başkanı & Tam Yetkili"
                                                                },
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
                                                            )
                                                        }
                                                        Surface(shape = CircleShape, color = LibraLavender) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                            ) {
                                                                Icon(Icons.Default.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                                Text("Başkan", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp), color = Color.White)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (conversation.createdAt > 0L) {
                                            val formatted = remember(conversation.createdAt) {
                                                SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("tr")).format(Date(conversation.createdAt))
                                            }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(16.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.Groups,
                                                        null,
                                                        tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Column {
                                                        Text("Kuruluş Tarihi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(formatted, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                                    }
                                                }
                                            }
                                        }

                                        // Quick Actions Deck
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Hızlı İşlemler",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Column {
                                                    if (canAddMembers) {
                                                        GroupActionRow(
                                                            icon = Icons.Default.PersonAdd,
                                                            title = "Gruba Kişi Ekle",
                                                            subtitle = "Arkadaşlarını veya diğer Libra üyelerini ekle",
                                                            tint = LibraLavender,
                                                            onClick = { page = GroupAboutPage.ADD_MEMBERS }
                                                        )
                                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                    }
                                                    GroupActionRow(
                                                        icon = Icons.Default.Share,
                                                        title = "Grup Davetini Paylaş",
                                                        subtitle = "Grubun bilgilerini arkadaşlarınla paylaş",
                                                        tint = Color(0xFF0284C7),
                                                        onClick = ::shareGroupInvite
                                                    )
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                    GroupActionRow(
                                                        icon = Icons.Default.DeleteOutline,
                                                        title = "Sohbet Geçmişini Sıfırla",
                                                        subtitle = "Kendi mesajlarını sil ve görünümü temizle",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        onClick = { confirmClearChat = true }
                                                    )
                                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                    GroupActionRow(
                                                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                                                        title = "Gruptan Ayrıl",
                                                        subtitle = if (currentUid == presidentUid) "Başkanlık otomatik devredilir" else "Bu gruptan çıkış yap",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        onClick = { confirmLeaveGroup = true }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    GroupAboutTab.MEMBERS -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = memberSearch,
                                                onValueChange = { memberSearch = it },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                                shape = RoundedCornerShape(14.dp),
                                                placeholder = { Text("Katılımcı ara…") },
                                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                                trailingIcon = if (memberSearch.isNotBlank()) {
                                                    {
                                                        IconButton(onClick = { memberSearch = "" }) {
                                                            Icon(Icons.Default.Close, "Temizle")
                                                        }
                                                    }
                                                } else null
                                            )
                                            if (canAddMembers) {
                                                Surface(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .appleBouncy(pressedScale = 0.9f, onClick = { page = GroupAboutPage.ADD_MEMBERS }),
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = LibraLavender
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Default.PersonAdd, "Kişi Ekle", tint = Color.White)
                                                    }
                                                }
                                            }
                                        }

                                        if (loadingProfiles && memberProfiles.isEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                                Spacer(Modifier.width(8.dp))
                                                Text("Katılımcılar yükleniyor…", style = MaterialTheme.typography.bodySmall)
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Column {
                                                    filteredMemberUids.forEachIndexed { index, uid ->
                                                        val profile = memberProfiles[uid] ?: UserProfile(uid = uid, displayName = "Libra Üyesi")
                                                        val role = resolveGroupMemberRole(conversation, uid)
                                                        val isSelf = uid == currentUid
                                                        var showMemberMenu by remember(uid) { mutableStateOf(false) }

                                                        val canToggleAdmin = currentUid == presidentUid && !isSelf
                                                        val canKickTarget = !isSelf &&
                                                            role != GroupMemberRole.PRESIDENT &&
                                                            isManager &&
                                                            (role != GroupMemberRole.ADMIN || currentUid == presidentUid)

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    onDismiss()
                                                                    onOpenProfile(uid)
                                                                }
                                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            UserAvatar(
                                                                photoUrl = profile.profileImageUrl,
                                                                initials = profile.initials,
                                                                size = 44.dp
                                                            )
                                                            Column(
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .padding(horizontal = 12.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = profile.displayName.ifBlank { "Libra Üyesi" } +
                                                                            if (isSelf) " (Sen)" else "",
                                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis,
                                                                        modifier = Modifier.weight(1f, fill = false)
                                                                    )
                                                                    when (role) {
                                                                        GroupMemberRole.PRESIDENT -> LibraRoleChip("Başkan", highlighted = true)
                                                                        GroupMemberRole.ADMIN -> LibraRoleChip("Yönetici", highlighted = false)
                                                                        GroupMemberRole.MEMBER -> Unit
                                                                    }
                                                                }
                                                                if (profile.username.isNotBlank()) {
                                                                    Text(
                                                                        text = "@${profile.username}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }

                                                            if (!isSelf && (canToggleAdmin || canKickTarget)) {
                                                                Box {
                                                                    IconButton(
                                                                        enabled = !actionBusy,
                                                                        onClick = { showMemberMenu = true }
                                                                    ) {
                                                                        Icon(Icons.Default.MoreVert, "Üye seçenekleri", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    }
                                                                    DropdownMenu(
                                                                        expanded = showMemberMenu,
                                                                        onDismissRequest = { showMemberMenu = false },
                                                                        containerColor = MaterialTheme.colorScheme.surface
                                                                    ) {
                                                                        DropdownMenuItem(
                                                                            text = { Text("Profili Gör") },
                                                                            leadingIcon = { Icon(Icons.Default.Person, null) },
                                                                            onClick = {
                                                                                showMemberMenu = false
                                                                                onDismiss()
                                                                                onOpenProfile(uid)
                                                                            }
                                                                        )
                                                                        if (canToggleAdmin) {
                                                                            val makeAdmin = role != GroupMemberRole.ADMIN
                                                                            DropdownMenuItem(
                                                                                text = { Text(if (makeAdmin) "Yönetici Yap" else "Yöneticilikten Çıkar") },
                                                                                leadingIcon = { Icon(Icons.Default.AdminPanelSettings, null) },
                                                                                onClick = {
                                                                                    showMemberMenu = false
                                                                                    actionBusy = true
                                                                                    onSetAdmin(uid, makeAdmin) { ok, err ->
                                                                                        actionBusy = false
                                                                                        val msg = if (ok) {
                                                                                            if (makeAdmin) "${profile.displayName} yönetici yapıldı."
                                                                                            else "${profile.displayName} yöneticilikten çıkarıldı."
                                                                                        } else err ?: "İşlem tamamlanamadı."
                                                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            )
                                                                        }
                                                                        if (canKickTarget) {
                                                                            DropdownMenuItem(
                                                                                text = { Text("Gruptan Çıkar", color = MaterialTheme.colorScheme.error) },
                                                                                leadingIcon = { Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error) },
                                                                                onClick = {
                                                                                    showMemberMenu = false
                                                                                    confirmKickMember = profile
                                                                                }
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            } else if (!isSelf) {
                                                                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                            }
                                                        }
                                                        if (index < filteredMemberUids.lastIndex) {
                                                            HorizontalDivider(
                                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                                                                modifier = Modifier.padding(start = 70.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    GroupAboutTab.MEDIA -> {
                                        if (mediaMessages.isEmpty() && sharedItemsMessages.isEmpty()) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(20.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                                                        modifier = Modifier.size(54.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                Icons.Default.PhotoLibrary,
                                                                null,
                                                                tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                                modifier = Modifier.size(26.dp)
                                                            )
                                                        }
                                                    }
                                                    Text("Henüz paylaşılan medya yok", fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "Grupta paylaşılan fotoğraflar, kitaplar, gönderiler ve bağlantılar burada düzenli biçimde görünür.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else {
                                            if (mediaMessages.isNotEmpty()) {
                                                Text(
                                                    text = "Fotoğraflar (${mediaMessages.size})",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    mediaMessages.chunked(3).forEach { rowItems ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            rowItems.forEach { msg ->
                                                                GroupMediaGridTile(
                                                                    mediaKey = msg.mediaUrl,
                                                                    onOpenFullscreen = { resolved ->
                                                                        fullscreenMediaUrl = resolved
                                                                    },
                                                                    modifier = Modifier.weight(1f)
                                                                )
                                                            }
                                                            repeat(3 - rowItems.size) {
                                                                Spacer(Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            if (sharedItemsMessages.isNotEmpty()) {
                                                Text(
                                                    text = "Paylaşımlar ve Bağlantılar (${sharedItemsMessages.size})",
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(18.dp),
                                                    color = MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                                ) {
                                                    Column {
                                                        sharedItemsMessages.take(25).forEachIndexed { idx, msg ->
                                                            val title = msg.sharedContent?.title?.ifBlank { null }
                                                                ?: msg.text.take(80)
                                                            val kind = when (msg.sharedContent?.type) {
                                                                "book" -> "Kitap"
                                                                "post" -> "Gönderi"
                                                                "story" -> "Hikâye"
                                                                else -> "Bağlantı / Paylaşım"
                                                            }
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Surface(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    color = if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface,
                                                                    modifier = Modifier.size(40.dp)
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Icon(
                                                                            Icons.Default.Link,
                                                                            null,
                                                                            tint = if (darkTheme) LibraLavenderDarkText else LibraLavenderText,
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                                Column(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .padding(horizontal = 12.dp)
                                                                ) {
                                                                    Text(
                                                                        text = title,
                                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                                        maxLines = 2,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                    Text(
                                                                        text = "$kind • ${msg.senderName.ifBlank { "Üye" }}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                            }
                                                            if (idx < sharedItemsMessages.take(25).lastIndex) {
                                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    GroupAboutTab.SETTINGS -> {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Grup İzinleri",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (!isManager) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                                    Text("Yalnızca yöneticiler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                        ) {
                                            Column {
                                                val applyPermissions: (GroupPermissions) -> Unit = { updated ->
                                                    val previous = permissionsDraft
                                                    permissionsDraft = updated
                                                    savingPerms = true
                                                    onUpdatePermissions(updated) { ok, err ->
                                                        savingPerms = false
                                                        if (!ok) {
                                                            permissionsDraft = previous
                                                            Toast.makeText(context, err ?: "İzinler güncellenemedi.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }

                                                PermissionToggleRow(
                                                    title = "Sadece Yöneticiler Mesaj Göndersin",
                                                    subtitle = "Açıkken normal üyeler yalnızca sohbeti okuyabilir.",
                                                    checked = permissionsDraft.onlyAdminsCanMessage,
                                                    enabled = isManager && !savingPerms,
                                                    onCheckedChange = {
                                                        applyPermissions(permissionsDraft.copy(onlyAdminsCanMessage = it))
                                                    }
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                PermissionToggleRow(
                                                    title = "Üyeler Kişi Ekleyebilsin",
                                                    subtitle = "Üyelerin gruba yeni katılımcı davet etmesine izin ver.",
                                                    checked = permissionsDraft.membersCanAddOthers,
                                                    enabled = isManager && !savingPerms,
                                                    onCheckedChange = {
                                                        applyPermissions(permissionsDraft.copy(membersCanAddOthers = it))
                                                    }
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                PermissionToggleRow(
                                                    title = "Üyeler Grup Bilgisini Düzenleyebilsin",
                                                    subtitle = "Grup adı, açıklaması, profil fotoğrafı ve kapağı değiştirme izni.",
                                                    checked = permissionsDraft.membersCanEditInfo,
                                                    enabled = isManager && !savingPerms,
                                                    onCheckedChange = {
                                                        applyPermissions(permissionsDraft.copy(membersCanEditInfo = it))
                                                    }
                                                )
                                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                                                PermissionToggleRow(
                                                    title = "Üyeler Medya Paylaşabilsin",
                                                    subtitle = "Kapalıysa yalnızca Başkan ve Yöneticiler fotoğraf atabilir.",
                                                    checked = permissionsDraft.membersCanSendMedia,
                                                    enabled = isManager && !savingPerms,
                                                    onCheckedChange = {
                                                        applyPermissions(permissionsDraft.copy(membersCanSendMedia = it))
                                                    }
                                                )
                                            }
                                        }

                                        if (canEditInfo && (conversation.groupPhotoUrl.isNotBlank() || conversation.groupBannerUrl.isNotBlank())) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text("Görsel Yönetimi", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        if (conversation.groupPhotoUrl.isNotBlank()) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    onUpdateMedia("", conversation.groupBannerUrl) { ok, _ ->
                                                                        if (ok) Toast.makeText(context, "Profil fotoğrafı kaldırıldı.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(12.dp)
                                                            ) { Text("Fotoğrafı Kaldır") }
                                                        }
                                                        if (conversation.groupBannerUrl.isNotBlank()) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    onUpdateMedia(conversation.groupPhotoUrl, "") { ok, _ ->
                                                                        if (ok) Toast.makeText(context, "Kapak kaldırıldı.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(12.dp)
                                                            ) { Text("Kapağı Kaldır") }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Danger Zone
                                        OutlinedButton(
                                            enabled = !actionBusy,
                                            onClick = { confirmLeaveGroup = true },
                                            modifier = Modifier.fillMaxWidth().height(48.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text("Gruptan Ayrıl", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fullscreenMediaUrl?.let { resolved ->
        FullScreenImageViewerDialog(
            mediaUrl = resolved,
            onDismiss = { fullscreenMediaUrl = null }
        )
    }

    confirmKickMember?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmKickMember = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Üyeyi Gruptan Çıkar", fontWeight = FontWeight.Bold) },
            text = {
                Text("${target.displayName.ifBlank { "Bu kullanıcı" }} gruptan çıkarılacak ve artık bu sohbete mesaj gönderemeyecek.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmKickMember = null
                        actionBusy = true
                        onRemoveMember(target.uid) { ok, err ->
                            actionBusy = false
                            val msg = if (ok) "${target.displayName} gruptan çıkarıldı."
                            else err ?: "Üye çıkarılamadı."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Çıkar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmKickMember = null }) { Text("Vazgeç") }
            }
        )
    }

    if (confirmClearChat) {
        AlertDialog(
            onDismissRequest = { confirmClearChat = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Sohbeti Sıfırla", fontWeight = FontWeight.Bold) },
            text = {
                Text("Bu grupta gönderdiğin mesajların kalıcı silinecek ve grup sohbet görünümün sıfırdan başlayacak.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmClearChat = false
                        onClearConversation { ok ->
                            if (ok) Toast.makeText(context, "Grup sohbeti sıfırlandı.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Sıfırla") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearChat = false }) { Text("Vazgeç") }
            }
        )
    }

    if (confirmLeaveGroup) {
        val isPresident = currentUid == presidentUid
        AlertDialog(
            onDismissRequest = { confirmLeaveGroup = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Gruptan Ayrıl", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (isPresident && sortedMemberUids.size > 1) {
                        "Grup Başkanı olduğun için ayrıldığında başkanlık sıradaki yöneticiye veya üyeye devredilecek."
                    } else {
                        "\"${conversation.groupName.ifBlank { "Bu gruptan" }}\" ayrılmak istediğine emin misin?"
                    }
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        confirmLeaveGroup = false
                        actionBusy = true
                        onRemoveMember(currentUid) { ok, err ->
                            actionBusy = false
                            if (ok) {
                                Toast.makeText(context, "Gruptan ayrıldın.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                                onLeaveCompleted()
                            } else {
                                Toast.makeText(context, err ?: "Gruptan ayrılınamadı.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) { Text("Ayrıl") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeaveGroup = false }) { Text("Vazgeç") }
            }
        )
    }
}

@Composable
private fun GroupMetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = tint.copy(alpha = 0.14f),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GroupMediaGridTile(
    mediaKey: String,
    onOpenFullscreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var signedUrl by remember(mediaKey) { mutableStateOf<String?>(null) }
    LaunchedEffect(mediaKey) {
        signedUrl = runCatching { ServiceLocator.storageRepository.getSignedMediaUrl(mediaKey) }.getOrNull()
    }
    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = !signedUrl.isNullOrBlank()) {
                signedUrl?.let(onOpenFullscreen)
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        LibraSignedImage(
            mediaKeyOrUrl = mediaKey,
            contentDescription = "Grup medyası",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun PermissionToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LibraLavender,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun AddGroupMembersFullPage(
    conversation: DirectConversation,
    currentUid: String,
    allConversations: List<DirectConversation>,
    onBack: () -> Unit,
    onShareInvite: () -> Unit,
    onConfirmAdd: (List<String>, (Boolean) -> Unit) -> Unit
) {
    val darkTheme = LocalLibraDarkTheme.current
    var query by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var suggestedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var selectedUsers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var adding by remember { mutableStateOf(false) }

    val existingMemberSet = remember(conversation.participantIds) { conversation.participantIds.toSet() }

    LaunchedEffect(currentUid, allConversations) {
        val fromDm = allConversations.mapNotNull { conv ->
            if (conv.isGroup || conv.otherUserId.isBlank()) null
            else UserProfile(
                uid = conv.otherUserId,
                displayName = conv.otherUserName,
                username = conv.otherUserUsername,
                profileImageUrl = conv.otherUserPhotoUrl
            )
        }
        val following = (ServiceLocator.userRepository.getFollowing(currentUid) as? AppResult.Success)
            ?.data.orEmpty()
        suggestedUsers = (fromDm + following)
            .filter { it.uid.isNotBlank() && it.uid != currentUid }
            .distinctBy { it.uid }
    }

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) {
            searchResults = emptyList()
        } else {
            ServiceLocator.userRepository.searchUsers(q).collect { res ->
                if (res is AppResult.Success) {
                    searchResults = res.data.filter { it.uid != currentUid }.take(25)
                }
            }
        }
    }

    val displayedUsers = remember(query, searchResults, suggestedUsers) {
        if (query.trim().length >= 2) searchResults else suggestedUsers
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Üye Ekle ve Davet Et",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = conversation.groupName.ifBlank { "Grup" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onShareInvite,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Paylaş", style = MaterialTheme.typography.labelMedium)
            }
        }

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("İsim veya @kullaniciadi ile ara…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (query.isNotBlank()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                    }
                }
            } else null
        )

        if (selectedUsers.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Eklenecek (${selectedUsers.size}): " +
                    selectedUsers.joinToString(", ") { it.displayName.ifBlank { "@${it.username}" } },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (darkTheme) LibraLavenderDarkText else LibraLavenderText
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (displayedUsers.isEmpty()) {
                item {
                    Text(
                        text = if (query.trim().length < 2) {
                            "Eklemek istediğin kişinin adını veya kullanıcı adını yaz."
                        } else {
                            "Aramana uygun kullanıcı bulunamadı."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                }
            } else {
                items(displayedUsers, key = { it.uid }) { user ->
                    val alreadyInGroup = user.uid in existingMemberSet
                    val isSelected = selectedUsers.any { it.uid == user.uid }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !alreadyInGroup && !adding) {
                                selectedUsers = if (isSelected) {
                                    selectedUsers.filterNot { it.uid == user.uid }
                                } else {
                                    selectedUsers + user
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isSelected -> if (darkTheme) LibraLavenderDarkSurface else LibraLavenderSurface
                            alreadyInGroup -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) LibraLavender else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                photoUrl = user.profileImageUrl,
                                initials = user.initials,
                                size = 42.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = user.displayName.ifBlank { "Libra Üyesi" },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.username.isNotBlank()) {
                                    Text(
                                        text = "@${user.username}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            when {
                                alreadyInGroup -> {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                        Text(
                                            text = "Grupta",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                isSelected -> {
                                    Surface(shape = CircleShape, color = LibraLavender, modifier = Modifier.size(26.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, "Seçili", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                else -> {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Transparent,
                                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                                        modifier = Modifier.size(26.dp)
                                    ) {}
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            enabled = selectedUsers.isNotEmpty() && !adding,
            onClick = {
                adding = true
                onConfirmAdd(selectedUsers.map { it.uid }) { ok ->
                    adding = false
                    if (ok) selectedUsers = emptyList()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LibraLavender)
        ) {
            if (adding) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
            } else {
                Text(
                    text = if (selectedUsers.isEmpty()) "Kişi Seç" else "Gruba Ekle (${selectedUsers.size})",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
